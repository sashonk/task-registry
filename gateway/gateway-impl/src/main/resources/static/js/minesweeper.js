const CLASSIC_SIZE = 10;
const CLASSIC_MINES = 15;
const STAR_SIZE = 13;
const STAR_MINES = 20;

const STAR_ROWS = [
  "......#......",
  ".....###.....",
  "#############",
  ".###########.",
  ".###########.",
  "..#########..",
  "..#########..",
  "..#########..",
  ".###########.",
  ".###########.",
  "#############",
  ".....###.....",
  "......#......"
];

function createStarMask() {
  return STAR_ROWS.join("").split("").map(cell => cell === "#");
}

function createMinesweeper({
  boardSize,
  mineCount,
  activeMask = null,
  boardId,
  minesId,
  timeId,
  statusId,
  resetId
}) {
  const cellCount = boardSize * boardSize;
  const mask = activeMask ?? Array(cellCount).fill(true);
  let cells = [];
  let started = false;
  let finished = false;
  let elapsedSeconds = 0;
  let timerId = null;
  let chordIndex = null;
  let suppressClick = false;
  let suppressContextMenu = false;
  let boardElement;
  let minesElement;
  let timeElement;
  let statusElement;
  let resetElement;

  function createEmptyBoard() {
    return mask.map(active => active ? {
      mine: false,
      exploded: false,
      revealed: false,
      flagged: false,
      adjacent: 0
    } : null);
  }

  function neighborsOf(index) {
    const row = Math.floor(index / boardSize);
    const column = index % boardSize;
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
          && neighborRow < boardSize
          && neighborColumn >= 0
          && neighborColumn < boardSize
        ) {
          const neighborIndex = neighborRow * boardSize + neighborColumn;
          if (cells[neighborIndex] !== null) {
            neighbors.push(neighborIndex);
          }
        }
      }
    }

    return neighbors;
  }

  function placeMines(safeIndex) {
    const candidates = cells
      .map((cell, index) => cell !== null && index !== safeIndex ? index : -1)
      .filter(index => index >= 0);

    for (let index = candidates.length - 1; index > 0; index -= 1) {
      const randomIndex = Math.floor(Math.random() * (index + 1));
      [candidates[index], candidates[randomIndex]] = [candidates[randomIndex], candidates[index]];
    }

    candidates.slice(0, mineCount).forEach(index => {
      cells[index].mine = true;
    });

    cells.forEach((cell, index) => {
      if (cell !== null && !cell.mine) {
        cell.adjacent = neighborsOf(index).filter(neighborIndex => cells[neighborIndex].mine).length;
      }
    });
  }

  function formatDisplay(value) {
    return String(Math.max(0, Math.min(999, value))).padStart(3, "0");
  }

  function flaggedCount() {
    return cells.filter(cell => cell?.flagged).length;
  }

  function updateIndicators() {
    minesElement.textContent = formatDisplay(mineCount - flaggedCount());
    timeElement.textContent = formatDisplay(elapsedSeconds);
  }

  function cellLabel(cell, index) {
    const row = Math.floor(index / boardSize) + 1;
    const column = index % boardSize + 1;

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
    boardElement.innerHTML = cells.map((cell, index) => {
      if (cell === null) {
        return '<span class="minesweeper-cell-spacer" aria-hidden="true"></span>';
      }

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
      if (cell?.mine) {
        cell.revealed = true;
      }
    });
  }

  function hasWon() {
    return cells.every(cell => cell === null || cell.mine || cell.revealed);
  }

  function setChordPreview(index, active) {
    neighborsOf(index).forEach(neighborIndex => {
      if (!cells[neighborIndex].revealed && !cells[neighborIndex].flagged) {
        boardElement
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
    resetElement.textContent = "😮";
  }

  function finishGame(won) {
    finished = true;
    stopTimer();

    if (won) {
      cells.forEach(cell => {
        if (cell?.mine) {
          cell.flagged = true;
        }
      });
      resetElement.textContent = "😎";
      statusElement.textContent = "Победа! Все безопасные клетки открыты.";
    } else {
      revealMines();
      resetElement.textContent = "☹";
      statusElement.textContent = "Вы попали на мину. Начните новую игру.";
    }
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
    resetElement.textContent = "🙂";

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

  function openCell(index) {
    if (finished || cells[index].flagged || cells[index].revealed) {
      return;
    }

    if (!started) {
      placeMines(index);
      started = true;
      startTimer();
      statusElement.textContent = "Игра идёт";
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

    if (!cell.flagged && flaggedCount() >= mineCount) {
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
    resetElement.textContent = "🙂";
    statusElement.textContent = "Откройте первую клетку";
    renderBoard();
  }

  function init() {
    boardElement = document.getElementById(boardId);
    minesElement = document.getElementById(minesId);
    timeElement = document.getElementById(timeId);
    statusElement = document.getElementById(statusId);
    resetElement = document.getElementById(resetId);
    if (!boardElement || !minesElement || !timeElement || !statusElement || !resetElement) {
      return;
    }

    boardElement.addEventListener("mousedown", event => {
      const cell = event.target.closest(".minesweeper-cell");
      if (cell && event.buttons === 3) {
        event.preventDefault();
        startChord(Number(cell.dataset.index));
      }
    });

    boardElement.addEventListener("click", event => {
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

    boardElement.addEventListener("contextmenu", event => {
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

    resetElement.addEventListener("click", resetGame);
    resetGame();
  }

  return { init };
}

export function initMinesweeper() {
  createMinesweeper({
    boardSize: CLASSIC_SIZE,
    mineCount: CLASSIC_MINES,
    boardId: "minesweeper-board",
    minesId: "minesweeper-mines",
    timeId: "minesweeper-time",
    statusId: "minesweeper-status",
    resetId: "minesweeper-reset"
  }).init();
}

export function initStarMinesweeper() {
  createMinesweeper({
    boardSize: STAR_SIZE,
    mineCount: STAR_MINES,
    activeMask: createStarMask(),
    boardId: "star-minesweeper-board",
    minesId: "star-minesweeper-mines",
    timeId: "star-minesweeper-time",
    statusId: "star-minesweeper-status",
    resetId: "star-minesweeper-reset"
  }).init();
}
