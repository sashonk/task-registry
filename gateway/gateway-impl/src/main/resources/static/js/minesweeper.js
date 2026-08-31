const BOARD_SIZE = 10;
const CELL_COUNT = BOARD_SIZE * BOARD_SIZE;
const MINE_COUNT = 15;

let cells = [];
let started = false;
let finished = false;
let elapsedSeconds = 0;
let timerId = null;
let chordIndex = null;
let suppressClick = false;
let suppressContextMenu = false;

function createEmptyBoard() {
  return Array.from({ length: CELL_COUNT }, () => ({
    mine: false,
    exploded: false,
    revealed: false,
    flagged: false,
    adjacent: 0
  }));
}

function neighborsOf(index) {
  const row = Math.floor(index / BOARD_SIZE);
  const column = index % BOARD_SIZE;
  const neighbors = [];

  for (let rowOffset = -1; rowOffset <= 1; rowOffset += 1) {
    for (let columnOffset = -1; columnOffset <= 1; columnOffset += 1) {
      if (rowOffset === 0 && columnOffset === 0) {
        continue;
      }

      const neighborRow = row + rowOffset;
      const neighborColumn = column + columnOffset;
      if (
        neighborRow >= 0
        && neighborRow < BOARD_SIZE
        && neighborColumn >= 0
        && neighborColumn < BOARD_SIZE
      ) {
        neighbors.push(neighborRow * BOARD_SIZE + neighborColumn);
      }
    }
  }

  return neighbors;
}

function placeMines(safeIndex) {
  const candidates = Array.from({ length: CELL_COUNT }, (_, index) => index)
    .filter(index => index !== safeIndex);

  for (let index = candidates.length - 1; index > 0; index -= 1) {
    const randomIndex = Math.floor(Math.random() * (index + 1));
    [candidates[index], candidates[randomIndex]] = [candidates[randomIndex], candidates[index]];
  }

  candidates.slice(0, MINE_COUNT).forEach(index => {
    cells[index].mine = true;
  });

  cells.forEach((cell, index) => {
    if (!cell.mine) {
      cell.adjacent = neighborsOf(index).filter(neighborIndex => cells[neighborIndex].mine).length;
    }
  });
}

function formatDisplay(value) {
  return String(Math.max(0, Math.min(999, value))).padStart(3, "0");
}

function flaggedCount() {
  return cells.filter(cell => cell.flagged).length;
}

function updateIndicators() {
  document.getElementById("minesweeper-mines").textContent = formatDisplay(MINE_COUNT - flaggedCount());
  document.getElementById("minesweeper-time").textContent = formatDisplay(elapsedSeconds);
}

function cellLabel(cell, index) {
  const row = Math.floor(index / BOARD_SIZE) + 1;
  const column = index % BOARD_SIZE + 1;

  if (cell.flagged && !cell.revealed) {
    return `Строка ${row}, столбец ${column}: установлен флаг`;
  }
  if (!cell.revealed) {
    return `Строка ${row}, столбец ${column}: закрыто`;
  }
  if (cell.mine) {
    return `Строка ${row}, столбец ${column}: мина`;
  }
  return `Строка ${row}, столбец ${column}: ${cell.adjacent || "пусто"}`;
}

function renderBoard() {
  const board = document.getElementById("minesweeper-board");
  board.innerHTML = cells.map((cell, index) => {
    const classes = ["minesweeper-cell"];
    let content = "";

    if (cell.revealed) {
      classes.push("revealed");
      if (cell.mine) {
        classes.push("mine");
        if (cell.exploded) {
          classes.push("exploded");
        }
        content = "✹";
      } else if (cell.adjacent > 0) {
        classes.push(`number-${cell.adjacent}`);
        content = String(cell.adjacent);
      }
    } else if (cell.flagged) {
      classes.push("flagged");
      content = "⚑";
    }

    return `
      <button
        type="button"
        class="${classes.join(" ")}"
        data-index="${index}"
        role="gridcell"
        aria-label="${cellLabel(cell, index)}"
      >${content}</button>
    `;
  }).join("");

  updateIndicators();
}

function startTimer() {
  if (timerId !== null) {
    return;
  }

  timerId = window.setInterval(() => {
    elapsedSeconds = Math.min(999, elapsedSeconds + 1);
    updateIndicators();
  }, 1000);
}

function stopTimer() {
  if (timerId !== null) {
    window.clearInterval(timerId);
    timerId = null;
  }
}

function revealArea(startIndex) {
  const queue = [startIndex];
  const queued = new Set(queue);

  while (queue.length > 0) {
    const index = queue.shift();
    const cell = cells[index];
    if (cell.revealed || cell.flagged || cell.mine) {
      continue;
    }

    cell.revealed = true;
    if (cell.adjacent === 0) {
      neighborsOf(index).forEach(neighborIndex => {
        if (!queued.has(neighborIndex)) {
          queued.add(neighborIndex);
          queue.push(neighborIndex);
        }
      });
    }
  }
}

function revealMines() {
  cells.forEach(cell => {
    if (cell.mine) {
      cell.revealed = true;
    }
  });
}

function hasWon() {
  return cells.every(cell => cell.mine || cell.revealed);
}

function setChordPreview(index, active) {
  neighborsOf(index).forEach(neighborIndex => {
    if (!cells[neighborIndex].revealed && !cells[neighborIndex].flagged) {
      document
        .querySelector(`.minesweeper-cell[data-index="${neighborIndex}"]`)
        ?.classList.toggle("chord-preview", active);
    }
  });
}

function startChord(index) {
  const cell = cells[index];
  if (finished || !cell.revealed || cell.adjacent === 0 || chordIndex !== null) {
    return;
  }

  chordIndex = index;
  setChordPreview(index, true);
  document.getElementById("minesweeper-reset").textContent = "😮";
}

function finishChord() {
  if (chordIndex === null) {
    return;
  }

  const index = chordIndex;
  setChordPreview(index, false);
  chordIndex = null;
  suppressClick = true;
  suppressContextMenu = true;
  document.getElementById("minesweeper-reset").textContent = "🙂";

  const neighbors = neighborsOf(index);
  const neighborFlags = neighbors.filter(neighborIndex => cells[neighborIndex].flagged).length;
  if (neighborFlags !== cells[index].adjacent) {
    return;
  }

  const hiddenNeighbors = neighbors.filter(neighborIndex => (
    !cells[neighborIndex].revealed && !cells[neighborIndex].flagged
  ));
  const mineIndex = hiddenNeighbors.find(neighborIndex => cells[neighborIndex].mine);

  if (mineIndex !== undefined) {
    cells[mineIndex].exploded = true;
    cells[mineIndex].revealed = true;
    finishGame(false);
  } else {
    hiddenNeighbors.forEach(revealArea);
    if (hasWon()) {
      finishGame(true);
    }
  }

  renderBoard();
}

function finishGame(won) {
  finished = true;
  stopTimer();

  const resetButton = document.getElementById("minesweeper-reset");
  const status = document.getElementById("minesweeper-status");
  if (won) {
    cells.forEach(cell => {
      if (cell.mine) {
        cell.flagged = true;
      }
    });
    resetButton.textContent = "😎";
    status.textContent = "Победа! Все безопасные клетки открыты.";
  } else {
    revealMines();
    resetButton.textContent = "☹";
    status.textContent = "Вы попали на мину. Начните новую игру.";
  }
}

function openCell(index) {
  if (finished || cells[index].flagged || cells[index].revealed) {
    return;
  }

  if (!started) {
    placeMines(index);
    started = true;
    startTimer();
    document.getElementById("minesweeper-status").textContent = "Игра идёт";
  }

  if (cells[index].mine) {
    cells[index].exploded = true;
    cells[index].revealed = true;
    finishGame(false);
  } else {
    revealArea(index);
    if (hasWon()) {
      finishGame(true);
    }
  }

  renderBoard();
}

function toggleFlag(index) {
  const cell = cells[index];
  if (finished || cell.revealed) {
    return;
  }

  if (!cell.flagged && flaggedCount() >= MINE_COUNT) {
    return;
  }

  cell.flagged = !cell.flagged;
  renderBoard();
}

function resetGame() {
  stopTimer();
  if (chordIndex !== null) {
    setChordPreview(chordIndex, false);
  }
  cells = createEmptyBoard();
  started = false;
  finished = false;
  elapsedSeconds = 0;
  chordIndex = null;
  suppressClick = false;
  suppressContextMenu = false;
  document.getElementById("minesweeper-reset").textContent = "🙂";
  document.getElementById("minesweeper-status").textContent = "Откройте первую клетку";
  renderBoard();
}

export function initMinesweeper() {
  const board = document.getElementById("minesweeper-board");
  const resetButton = document.getElementById("minesweeper-reset");
  if (!board || !resetButton) {
    return;
  }

  board.addEventListener("mousedown", event => {
    const cell = event.target.closest(".minesweeper-cell");
    if (cell && event.buttons === 3) {
      event.preventDefault();
      startChord(Number(cell.dataset.index));
    }
  });

  board.addEventListener("click", event => {
    if (suppressClick) {
      suppressClick = false;
      event.preventDefault();
      return;
    }

    const cell = event.target.closest(".minesweeper-cell");
    if (cell) {
      openCell(Number(cell.dataset.index));
    }
  });

  board.addEventListener("contextmenu", event => {
    const cell = event.target.closest(".minesweeper-cell");
    if (cell) {
      event.preventDefault();
      if (suppressContextMenu) {
        suppressContextMenu = false;
        return;
      }
      toggleFlag(Number(cell.dataset.index));
    }
  });

  document.addEventListener("mouseup", event => {
    if (chordIndex !== null && event.buttons !== 3) {
      finishChord();
    }

    if (event.buttons === 0) {
      window.setTimeout(() => {
        suppressClick = false;
        suppressContextMenu = false;
      }, 0);
    }
  });

  resetButton.addEventListener("click", resetGame);
  resetGame();
}
