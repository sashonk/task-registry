const BOARD_SIZE = 9;
const CELL_COUNT = BOARD_SIZE * BOARD_SIZE;
const STARTING_BALLS = 5;
const BALLS_PER_TURN = 3;
const LINE_LENGTH = 5;
const COLORS = ["red", "yellow", "green", "cyan", "blue", "purple", "pink"];
const COLOR_NAMES = {
  red: "красный",
  yellow: "жёлтый",
  green: "зелёный",
  cyan: "голубой",
  blue: "синий",
  purple: "фиолетовый",
  pink: "розовый"
};

let board = [];
let selectedIndex = null;
let nextColors = [];
let score = 0;
let finished = false;

function randomColor() {
  return COLORS[Math.floor(Math.random() * COLORS.length)];
}

function createColors(count) {
  return Array.from({ length: count }, randomColor);
}

function emptyIndices() {
  return board
    .map((color, index) => color === null ? index : -1)
    .filter(index => index >= 0);
}

function takeRandom(items) {
  const index = Math.floor(Math.random() * items.length);
  return items.splice(index, 1)[0];
}

function spawnBalls(colors) {
  const empty = emptyIndices();
  colors.forEach(color => {
    if (empty.length > 0) {
      board[takeRandom(empty)] = color;
    }
  });
}

function coordinates(index) {
  return {
    row: Math.floor(index / BOARD_SIZE),
    column: index % BOARD_SIZE
  };
}

function indexAt(row, column) {
  if (row < 0 || row >= BOARD_SIZE || column < 0 || column >= BOARD_SIZE) {
    return null;
  }
  return row * BOARD_SIZE + column;
}

function orthogonalNeighbors(index) {
  const { row, column } = coordinates(index);
  return [
    indexAt(row - 1, column),
    indexAt(row + 1, column),
    indexAt(row, column - 1),
    indexAt(row, column + 1)
  ].filter(neighbor => neighbor !== null);
}

function hasPath(fromIndex, toIndex) {
  const queue = [fromIndex];
  const visited = new Set(queue);

  while (queue.length > 0) {
    const index = queue.shift();
    if (index === toIndex) {
      return true;
    }

    orthogonalNeighbors(index).forEach(neighbor => {
      if (!visited.has(neighbor) && (board[neighbor] === null || neighbor === toIndex)) {
        visited.add(neighbor);
        queue.push(neighbor);
      }
    });
  }

  return false;
}

function completedLineIndices() {
  const completed = new Set();
  const directions = [
    [0, 1],
    [1, 0],
    [1, 1],
    [1, -1]
  ];

  board.forEach((color, index) => {
    if (color === null) {
      return;
    }

    const { row, column } = coordinates(index);
    directions.forEach(([rowStep, columnStep]) => {
      const previousIndex = indexAt(row - rowStep, column - columnStep);
      if (previousIndex !== null && board[previousIndex] === color) {
        return;
      }

      const line = [];
      let lineRow = row;
      let lineColumn = column;
      let lineIndex = indexAt(lineRow, lineColumn);
      while (lineIndex !== null && board[lineIndex] === color) {
        line.push(lineIndex);
        lineRow += rowStep;
        lineColumn += columnStep;
        lineIndex = indexAt(lineRow, lineColumn);
      }

      if (line.length >= LINE_LENGTH) {
        line.forEach(lineCellIndex => completed.add(lineCellIndex));
      }
    });
  });

  return completed;
}

function removeCompletedLines() {
  const completed = completedLineIndices();
  completed.forEach(index => {
    board[index] = null;
  });
  score += completed.size;
  return completed.size;
}

function formatScore() {
  return String(score).padStart(3, "0");
}

function cellLabel(color, index) {
  const { row, column } = coordinates(index);
  const position = `Строка ${row + 1}, столбец ${column + 1}`;
  if (color === null) {
    return `${position}: пусто`;
  }
  return `${position}: ${COLOR_NAMES[color]} шар`;
}

function renderNextColors() {
  document.getElementById("color-lines-next").innerHTML = nextColors
    .map(color => `
      <span
        class="color-lines-ball color-${color}"
        role="img"
        aria-label="${COLOR_NAMES[color]} шар"
      ></span>
    `)
    .join("");
}

function renderBoard() {
  document.getElementById("color-lines-board").innerHTML = board
    .map((color, index) => `
      <button
        type="button"
        class="color-lines-cell${selectedIndex === index ? " selected" : ""}"
        data-index="${index}"
        role="gridcell"
        aria-label="${cellLabel(color, index)}"
        aria-pressed="${selectedIndex === index}"
      >${color === null ? "" : `<span class="color-lines-ball color-${color}"></span>`}</button>
    `)
    .join("");

  document.getElementById("color-lines-score").textContent = formatScore();
  renderNextColors();
}

function finishIfBoardIsFull() {
  if (emptyIndices().length > 0) {
    return false;
  }

  finished = true;
  selectedIndex = null;
  document.getElementById("color-lines-status").textContent =
    `Игра окончена. Итоговый счёт: ${score}.`;
  return true;
}

function completeTurn() {
  let removed = removeCompletedLines();
  if (removed > 0) {
    document.getElementById("color-lines-status").textContent = `Удалено шаров: ${removed}`;
    finishIfBoardIsFull();
    renderBoard();
    return;
  }

  spawnBalls(nextColors);
  nextColors = createColors(BALLS_PER_TURN);
  removed = removeCompletedLines();
  if (removed > 0) {
    document.getElementById("color-lines-status").textContent =
      `Новые шары образовали линию. Удалено: ${removed}`;
  } else {
    document.getElementById("color-lines-status").textContent = "Появились три новых шара";
  }

  finishIfBoardIsFull();
  renderBoard();
}

function selectOrMove(index) {
  if (finished) {
    return;
  }

  if (board[index] !== null) {
    selectedIndex = index;
    document.getElementById("color-lines-status").textContent =
      `Выбран ${COLOR_NAMES[board[index]]} шар`;
    renderBoard();
    return;
  }

  if (selectedIndex === null) {
    document.getElementById("color-lines-status").textContent = "Сначала выберите шар";
    return;
  }

  if (!hasPath(selectedIndex, index)) {
    document.getElementById("color-lines-status").textContent = "К этой клетке нет свободного пути";
    return;
  }

  board[index] = board[selectedIndex];
  board[selectedIndex] = null;
  selectedIndex = null;
  completeTurn();
}

function resetGame() {
  do {
    board = Array(CELL_COUNT).fill(null);
    spawnBalls(createColors(STARTING_BALLS));
  } while (completedLineIndices().size > 0);

  selectedIndex = null;
  nextColors = createColors(BALLS_PER_TURN);
  score = 0;
  finished = false;
  document.getElementById("color-lines-status").textContent = "Выберите шар";
  renderBoard();
}

export function initColorLines() {
  const gameBoard = document.getElementById("color-lines-board");
  const resetButton = document.getElementById("color-lines-reset");
  if (!gameBoard || !resetButton) {
    return;
  }

  gameBoard.addEventListener("click", event => {
    const cell = event.target.closest(".color-lines-cell");
    if (cell) {
      selectOrMove(Number(cell.dataset.index));
    }
  });

  resetButton.addEventListener("click", resetGame);
  resetGame();
}
