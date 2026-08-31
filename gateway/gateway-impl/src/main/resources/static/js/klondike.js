const SUITS = ["spades", "hearts", "diamonds", "clubs"];
const RANK_LABELS = ["A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"];
const SUIT_SYMBOLS = {
  spades: "♠",
  hearts: "♥",
  diamonds: "♦",
  clubs: "♣"
};
const RED_SUITS = new Set(["hearts", "diamonds"]);
const DRAW_COUNT = 3;

let tableau = [];
let foundations = [];
let stock = [];
let waste = [];
let selected = null;
let moves = 0;
let elapsedSeconds = 0;
let timerId = null;
let won = false;

function isRed(suit) {
  return RED_SUITS.has(suit);
}

function suitIndex(suit) {
  return SUITS.indexOf(suit);
}

function createDeck() {
  const deck = [];
  for (const suit of SUITS) {
    for (let rank = 1; rank <= 13; rank += 1) {
      deck.push({ suit, rank, faceUp: false });
    }
  }
  return deck;
}

function shuffle(deck) {
  for (let index = deck.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1));
    [deck[index], deck[swapIndex]] = [deck[swapIndex], deck[index]];
  }
}

function dealNewGame() {
  const deck = createDeck();
  shuffle(deck);

  tableau = Array.from({ length: 7 }, () => []);
  foundations = Array.from({ length: 4 }, () => []);
  stock = [];
  waste = [];

  let deckIndex = 0;
  for (let column = 0; column < 7; column += 1) {
    for (let row = 0; row <= column; row += 1) {
      const card = deck[deckIndex];
      deckIndex += 1;
      card.faceUp = row === column;
      tableau[column].push(card);
    }
  }

  for (; deckIndex < deck.length; deckIndex += 1) {
    stock.push(deck[deckIndex]);
  }

  selected = null;
  moves = 0;
  won = false;
  resetTimer();
  startTimer();
}

function resetTimer() {
  stopTimer();
  elapsedSeconds = 0;
  updateTimerDisplay();
}

function startTimer() {
  timerId = window.setInterval(() => {
    if (!won) {
      elapsedSeconds += 1;
      updateTimerDisplay();
    }
  }, 1000);
}

function stopTimer() {
  if (timerId !== null) {
    window.clearInterval(timerId);
    timerId = null;
  }
}

function formatCounter(value, digits = 3) {
  return String(value).padStart(digits, "0");
}

function updateTimerDisplay() {
  document.getElementById("klondike-time").textContent = formatCounter(elapsedSeconds);
}

function updateMovesDisplay() {
  document.getElementById("klondike-moves").textContent = formatCounter(moves);
}

function topCard(pile) {
  return pile.length > 0 ? pile[pile.length - 1] : null;
}

function revealTopTableau(column) {
  const pile = tableau[column];
  if (pile.length === 0) {
    return;
  }
  const card = pile[pile.length - 1];
  if (!card.faceUp) {
    card.faceUp = true;
  }
}

function isValidTableauSequence(cards) {
  if (cards.length === 0 || !cards.every(card => card.faceUp)) {
    return false;
  }

  for (let index = 1; index < cards.length; index += 1) {
    const previous = cards[index - 1];
    const current = cards[index];
    if (previous.rank !== current.rank + 1) {
      return false;
    }
    if (isRed(previous.suit) === isRed(current.suit)) {
      return false;
    }
  }

  return true;
}

function canPlaceOnTableau(movingCards, targetColumn) {
  if (movingCards.length === 0 || !isValidTableauSequence(movingCards)) {
    return false;
  }

  const card = movingCards[0];
  const targetPile = tableau[targetColumn];
  if (targetPile.length === 0) {
    return card.rank === 13;
  }

  const targetCard = topCard(targetPile);
  return targetCard.rank === card.rank + 1 && isRed(targetCard.suit) !== isRed(card.suit);
}

function canMoveToFoundation(card, foundationColumn) {
  const pile = foundations[foundationColumn];
  if (pile.length === 0) {
    return card.rank === 1;
  }

  const targetCard = topCard(pile);
  return targetCard.suit === card.suit && card.rank === targetCard.rank + 1;
}

function foundationForSuit(suit) {
  return suitIndex(suit);
}

function autoFoundationTarget(card) {
  const column = foundationForSuit(card.suit);
  return canMoveToFoundation(card, column) ? column : null;
}

function checkWin() {
  return foundations.every(pile => pile.length === 13);
}

function setStatus(message) {
  document.getElementById("klondike-status").textContent = message;
}

function clearSelection() {
  selected = null;
}

function registerMove() {
  moves += 1;
  updateMovesDisplay();
}

function drawFromStock() {
  clearSelection();

  if (stock.length === 0) {
    if (waste.length === 0) {
      setStatus("Колода пуста");
      renderBoard();
      return;
    }

    while (waste.length > 0) {
      const card = waste.pop();
      card.faceUp = false;
      stock.push(card);
    }
    setStatus("Колода перевернута");
    renderBoard();
    return;
  }

  const count = Math.min(DRAW_COUNT, stock.length);
  for (let index = 0; index < count; index += 1) {
    const card = stock.pop();
    card.faceUp = true;
    waste.push(card);
  }

  registerMove();
  setStatus("Взято из колоды");
  renderBoard();
}

function moveWasteToFoundation() {
  if (waste.length === 0) {
    return false;
  }

  const card = topCard(waste);
  const target = autoFoundationTarget(card);
  if (target === null) {
    return false;
  }

  foundations[target].push(waste.pop());
  registerMove();
  return true;
}

function moveTableauToFoundation(column) {
  const pile = tableau[column];
  if (pile.length === 0) {
    return false;
  }

  const card = topCard(pile);
  if (!card.faceUp) {
    return false;
  }

  const target = autoFoundationTarget(card);
  if (target === null) {
    return false;
  }

  foundations[target].push(pile.pop());
  revealTopTableau(column);
  registerMove();
  return true;
}

function moveCards(sourceColumn, fromIndex, targetColumn) {
  const movingCards = tableau[sourceColumn].slice(fromIndex);
  if (!canPlaceOnTableau(movingCards, targetColumn)) {
    return false;
  }

  tableau[sourceColumn] = tableau[sourceColumn].slice(0, fromIndex);
  tableau[targetColumn].push(...movingCards);
  revealTopTableau(sourceColumn);
  registerMove();
  return true;
}

function moveWasteToTableau(targetColumn) {
  if (waste.length === 0) {
    return false;
  }

  const card = topCard(waste);
  if (!canPlaceOnTableau([card], targetColumn)) {
    return false;
  }

  tableau[targetColumn].push(waste.pop());
  registerMove();
  return true;
}

function finishIfWon() {
  if (!checkWin()) {
    return;
  }

  won = true;
  stopTimer();
  setStatus("Победа!");
}

function renderCard(card, options = {}) {
  const {
    selected: isSelected = false,
    stackOffset = 0,
    stackIndex = 0,
    clickable = true,
    dataSource = "",
    dataPile = "",
    dataIndex = ""
  } = options;

  const classes = ["klondike-card"];
  if (!card.faceUp) {
    classes.push("face-down");
  } else {
    classes.push(isRed(card.suit) ? "red" : "black");
  }
  if (isSelected) {
    classes.push("selected");
  }
  if (clickable) {
    classes.push("clickable");
  }

  const styleParts = [];
  if (stackOffset > 0) {
    styleParts.push(`--stack-offset:${stackOffset}px`);
  }
  if (stackIndex > 0) {
    styleParts.push(`--stack-index:${stackIndex}`);
  }
  const style = styleParts.length > 0 ? ` style="${styleParts.join("; ")}"` : "";
  const dataAttrs = [
    dataSource ? `data-source="${dataSource}"` : "",
    dataPile !== "" ? `data-pile="${dataPile}"` : "",
    dataIndex !== "" ? `data-index="${dataIndex}"` : ""
  ].filter(Boolean).join(" ");

  if (!card.faceUp) {
    return `<button type="button" class="${classes.join(" ")}" ${dataAttrs}${style} aria-label="Закрытая карта"></button>`;
  }

  const rank = RANK_LABELS[card.rank - 1];
  const symbol = SUIT_SYMBOLS[card.suit];
  return `<button type="button" class="${classes.join(" ")}" ${dataAttrs}${style} aria-label="${rank} ${symbol}">
    <span class="klondike-card-corner top">${rank}<span class="klondike-card-suit">${symbol}</span></span>
    <span class="klondike-card-center">${symbol}</span>
    <span class="klondike-card-corner bottom">${rank}<span class="klondike-card-suit">${symbol}</span></span>
  </button>`;
}

function renderEmptySlot(label, dataSource, dataPile) {
  return `<button type="button" class="klondike-slot" data-source="${dataSource}" data-pile="${dataPile}" aria-label="${label}"></button>`;
}

function renderBoard() {
  const foundationsEl = document.getElementById("klondike-foundations");
  const stockEl = document.getElementById("klondike-stock");
  const wasteEl = document.getElementById("klondike-waste");
  const tableauEl = document.getElementById("klondike-tableau");

  foundationsEl.innerHTML = foundations.map((pile, index) => {
    if (pile.length === 0) {
      return renderEmptySlot("Дом", "foundation", index);
    }
    const card = topCard(pile);
    const isSelected = selected?.type === "foundation" && selected.pile === index;
    return renderCard(card, {
      selected: isSelected,
      dataSource: "foundation",
      dataPile: index
    });
  }).join("");

  stockEl.innerHTML = stock.length > 0
    ? renderCard(topCard(stock), { clickable: true, dataSource: "stock", dataPile: 0 })
    : renderEmptySlot("Колода", "stock", 0);

  if (waste.length === 0) {
    wasteEl.innerHTML = renderEmptySlot("Сброс", "waste", 0);
  } else {
    const visibleCount = Math.min(3, waste.length);
    const startIndex = waste.length - visibleCount;
    wasteEl.innerHTML = waste.slice(startIndex).map((card, offset) => {
      const absoluteIndex = startIndex + offset;
      const isTop = absoluteIndex === waste.length - 1;
      return renderCard(card, {
        selected: selected?.type === "waste" && isTop,
        stackOffset: offset * 18,
        stackIndex: offset + 1,
        clickable: isTop,
        dataSource: "waste",
        dataPile: 0,
        dataIndex: absoluteIndex
      });
    }).join("");
  }

  tableauEl.innerHTML = tableau.map((pile, column) => {
    if (pile.length === 0) {
      return `<div class="klondike-column">${renderEmptySlot("Столбец", "tableau", column)}</div>`;
    }

    const cardsHtml = pile.map((card, index) => {
      const isSelected = selected?.type === "tableau"
        && selected.pile === column
        && index >= selected.fromIndex;
      return renderCard(card, {
        selected: isSelected,
        stackOffset: index * 18,
        stackIndex: index + 1,
        clickable: card.faceUp,
        dataSource: "tableau",
        dataPile: column,
        dataIndex: index
      });
    }).join("");

    return `<div class="klondike-column">${cardsHtml}${renderEmptySlot("Столбец", "tableau", column)}</div>`;
  }).join("");

  updateMovesDisplay();
  finishIfWon();
}

function handleFoundationClick(foundationColumn) {
  if (selected === null) {
    return;
  }

  if (selected.type === "waste") {
    if (moveWasteToFoundation()) {
      clearSelection();
      setStatus("Карта перенесена в дом");
      renderBoard();
    } else {
      setStatus("Сюда нельзя положить эту карту");
    }
    return;
  }

  if (selected.type === "tableau") {
    const pile = tableau[selected.pile];
    if (selected.fromIndex !== pile.length - 1) {
      setStatus("В дом можно положить только верхнюю карту");
      clearSelection();
      renderBoard();
      return;
    }

    const card = topCard(pile);
    if (foundationColumn !== foundationForSuit(card.suit) || !canMoveToFoundation(card, foundationColumn)) {
      setStatus("Сюда нельзя положить эту карту");
      clearSelection();
      renderBoard();
      return;
    }

    foundations[foundationColumn].push(pile.pop());
    revealTopTableau(selected.pile);
    registerMove();
    clearSelection();
    setStatus("Карта перенесена в дом");
    renderBoard();
  }
}

function handleTableauClick(column, cardIndex) {
  if (selected === null) {
    const pile = tableau[column];
    if (cardIndex === null || cardIndex === undefined) {
      return;
    }
    const card = pile[cardIndex];
    if (!card?.faceUp) {
      return;
    }
    selected = { type: "tableau", pile: column, fromIndex: cardIndex };
    setStatus("Выберите место для карты");
    renderBoard();
    return;
  }

  if (selected.type === "tableau") {
    if (selected.pile === column && selected.fromIndex === cardIndex) {
      clearSelection();
      setStatus("Выберите карту");
      renderBoard();
      return;
    }

    if (moveCards(selected.pile, selected.fromIndex, column)) {
      clearSelection();
      setStatus("Карты перенесены");
      renderBoard();
      return;
    }
  }

  if (selected.type === "waste") {
    if (moveWasteToTableau(column)) {
      clearSelection();
      setStatus("Карта перенесена на стол");
      renderBoard();
      return;
    }
  }

  setStatus("Сюда нельзя положить эти карты");
  clearSelection();
  renderBoard();
}

function handleTableauEmptyClick(column) {
  if (selected === null) {
    return;
  }

  if (selected.type === "tableau") {
    if (moveCards(selected.pile, selected.fromIndex, column)) {
      clearSelection();
      setStatus("Карты перенесены");
      renderBoard();
      return;
    }
  }

  if (selected.type === "waste") {
    if (moveWasteToTableau(column)) {
      clearSelection();
      setStatus("Карта перенесена на стол");
      renderBoard();
      return;
    }
  }

  setStatus("Сюда нельзя положить эти карты");
  clearSelection();
  renderBoard();
}

function handleWasteClick() {
  if (waste.length === 0) {
    return;
  }

  if (selected?.type === "waste") {
    clearSelection();
    setStatus("Выберите карту");
    renderBoard();
    return;
  }

  selected = { type: "waste" };
  setStatus("Выберите место для карты");
  renderBoard();
}

function handleDoubleClick(source, pile, index) {
  if (source === "waste") {
    if (moveWasteToFoundation()) {
      clearSelection();
      setStatus("Карта перенесена в дом");
      renderBoard();
    }
    return;
  }

  if (source === "tableau") {
    const column = Number(pile);
    const pileCards = tableau[column];
    if (Number(index) !== pileCards.length - 1) {
      return;
    }
    if (moveTableauToFoundation(column)) {
      clearSelection();
      setStatus("Карта перенесена в дом");
      renderBoard();
    }
  }
}

function handleBoardClick(event) {
  if (won) {
    return;
  }

  const target = event.target.closest("[data-source]");
  if (!target) {
    return;
  }

  const source = target.dataset.source;
  const pile = target.dataset.pile;
  const index = target.dataset.index;

  if (source === "stock") {
    drawFromStock();
    return;
  }

  if (source === "foundation") {
    handleFoundationClick(Number(pile));
    return;
  }

  if (source === "waste") {
    if (target.classList.contains("klondike-slot") || !target.classList.contains("clickable")) {
      return;
    }
    handleWasteClick();
    return;
  }

  if (source === "tableau") {
    if (target.classList.contains("klondike-slot")) {
      handleTableauEmptyClick(Number(pile));
      return;
    }
    handleTableauClick(Number(pile), Number(index));
  }
}

function handleBoardDoubleClick(event) {
  if (won) {
    return;
  }

  const target = event.target.closest(".klondike-card.clickable");
  if (!target) {
    return;
  }

  handleDoubleClick(target.dataset.source, target.dataset.pile, target.dataset.index);
}

export function initKlondike() {
  const board = document.getElementById("klondike-board");
  const resetButton = document.getElementById("klondike-reset");
  if (!board || !resetButton) {
    return;
  }

  board.addEventListener("click", handleBoardClick);
  board.addEventListener("dblclick", handleBoardDoubleClick);
  resetButton.addEventListener("click", () => {
    dealNewGame();
    setStatus("Выберите карту или возьмите из колоды");
    renderBoard();
  });

  dealNewGame();
  setStatus("Выберите карту или возьмите из колоды");
  renderBoard();
}
