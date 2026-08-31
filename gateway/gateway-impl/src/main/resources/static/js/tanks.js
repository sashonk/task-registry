const GRID_SIZE = 13;
const TILE_SIZE = 20;
const CANVAS_SIZE = GRID_SIZE * TILE_SIZE;

const CELL_EMPTY = 0;
const CELL_BRICK = 1;
const CELL_STEEL = 2;

const DIRECTIONS = [
  { dx: 0, dy: -1, name: "up" },
  { dx: 1, dy: 0, name: "right" },
  { dx: 0, dy: 1, name: "down" },
  { dx: -1, dy: 0, name: "left" }
];

const MAP_LAYOUT = [
  "#############",
  "#...........#",
  "#..+++..++..#",
  "#...........#",
  "#..+.....+..#",
  "#...........#",
  "##.###.###.##",
  "#...........#",
  "#..+.....+..#",
  "#...........#",
  "#..+++..++..#",
  "#...........#",
  "#############"
];

const PLAYER_SPAWN = { x: 6, y: 11, dir: 0 };
const ENEMY_SPAWNS = [
  { x: 2, y: 1, dir: 2 },
  { x: 6, y: 1, dir: 2 },
  { x: 10, y: 1, dir: 2 }
];

const TANK_MOVE_INTERVAL_MS = 140;
const BULLET_SPEED = 0.18;
const SHOOT_COOLDOWN_MS = 400;
const ENEMY_MOVE_INTERVAL_MS = 450;
const ENEMY_SHOOT_CHANCE = 0.25;

let canvas;
let context;
let walls = [];
let player = null;
let enemies = [];
let bullets = [];
let score = 0;
let gameOver = false;
let won = false;
let animationId = null;
let lastTimestamp = 0;
let pageElement = null;

const pressedKeys = new Set();
const keyDirection = {
  ArrowUp: 0,
  ArrowRight: 1,
  ArrowDown: 2,
  ArrowLeft: 3,
  KeyW: 0,
  KeyD: 1,
  KeyS: 2,
  KeyA: 3
};

function cloneMap() {
  return MAP_LAYOUT.map(row => row.split("").map(cell => {
    if (cell === "#") {
      return CELL_STEEL;
    }
    if (cell === "+") {
      return CELL_BRICK;
    }
    return CELL_EMPTY;
  }));
}

function createTank(spawn, isPlayer) {
  return {
    x: spawn.x,
    y: spawn.y,
    dir: spawn.dir,
    isPlayer,
    alive: true,
    moveCooldown: 0,
    shootCooldown: 0
  };
}

function resetGame() {
  walls = cloneMap();
  player = createTank(PLAYER_SPAWN, true);
  enemies = ENEMY_SPAWNS.map(spawn => createTank(spawn, false));
  bullets = [];
  score = 0;
  gameOver = false;
  won = false;
  updateScoreDisplay();
  setStatus("Уничтожьте всех врагов");
}

function setStatus(message) {
  const statusElement = document.getElementById("tanks-status");
  if (statusElement) {
    statusElement.textContent = message;
  }
}

function updateScoreDisplay() {
  const scoreElement = document.getElementById("tanks-score");
  if (scoreElement) {
    scoreElement.textContent = String(score).padStart(3, "0");
  }
}

function isInsideGrid(x, y) {
  return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
}

function cellAt(x, y) {
  if (!isInsideGrid(x, y)) {
    return CELL_STEEL;
  }
  return walls[y][x];
}

function tankAt(x, y, ignoreTank = null) {
  if (player.alive && player !== ignoreTank && player.x === x && player.y === y) {
    return player;
  }

  return enemies.find(enemy => enemy.alive && enemy !== ignoreTank && enemy.x === x && enemy.y === y) ?? null;
}

function canTankMoveTo(x, y, tank) {
  const cell = cellAt(x, y);
  if (cell === CELL_BRICK || cell === CELL_STEEL) {
    return false;
  }
  return tankAt(x, y, tank) === null;
}

function tryMoveTank(tank) {
  const direction = DIRECTIONS[tank.dir];
  const nextX = tank.x + direction.dx;
  const nextY = tank.y + direction.dy;
  if (!canTankMoveTo(nextX, nextY, tank)) {
    return false;
  }

  tank.x = nextX;
  tank.y = nextY;
  return true;
}

function shootFromTank(tank) {
  if (tank.shootCooldown > 0 || !tank.alive) {
    return;
  }

  const direction = DIRECTIONS[tank.dir];
  const bulletX = tank.x + 0.5 + direction.dx * 0.35;
  const bulletY = tank.y + 0.5 + direction.dy * 0.35;

  bullets.push({
    x: bulletX,
    y: bulletY,
    dir: tank.dir,
    ownerPlayer: tank.isPlayer
  });

  tank.shootCooldown = SHOOT_COOLDOWN_MS;
}

function destroyTank(tank) {
  tank.alive = false;
  if (!tank.isPlayer) {
    score += 1;
    updateScoreDisplay();
  }
}

function updateBullets(deltaMs) {
  bullets = bullets.filter(bullet => {
    const direction = DIRECTIONS[bullet.dir];
    const steps = (deltaMs / 16) * BULLET_SPEED;
    bullet.x += direction.dx * steps;
    bullet.y += direction.dy * steps;

    const gridX = Math.floor(bullet.x);
    const gridY = Math.floor(bullet.y);
    const cell = cellAt(gridX, gridY);

    if (cell === CELL_STEEL || !isInsideGrid(gridX, gridY)) {
      return false;
    }

    if (cell === CELL_BRICK) {
      walls[gridY][gridX] = CELL_EMPTY;
      return false;
    }

    if (player.alive && player.x === gridX && player.y === gridY) {
      if (!bullet.ownerPlayer) {
        destroyTank(player);
        gameOver = true;
        setStatus("Поражение");
      }
      return false;
    }

    const hitEnemy = enemies.find(enemy => enemy.alive && enemy.x === gridX && enemy.y === gridY);
    if (hitEnemy) {
      if (bullet.ownerPlayer) {
        destroyTank(hitEnemy);
      }
      return false;
    }

    return bullet.x >= -0.5 && bullet.x <= GRID_SIZE + 0.5
      && bullet.y >= -0.5 && bullet.y <= GRID_SIZE + 0.5;
  });
}

function updatePlayer(deltaMs) {
  if (!player.alive || gameOver) {
    return;
  }

  player.shootCooldown = Math.max(0, player.shootCooldown - deltaMs);
  player.moveCooldown = Math.max(0, player.moveCooldown - deltaMs);

  for (const [key, direction] of Object.entries(keyDirection)) {
    if (!pressedKeys.has(key)) {
      continue;
    }

    if (player.dir !== direction) {
      player.dir = direction;
      player.moveCooldown = 0;
    }

    if (player.moveCooldown <= 0) {
      if (tryMoveTank(player)) {
        player.moveCooldown = TANK_MOVE_INTERVAL_MS;
      }
    }
    break;
  }

  if (pressedKeys.has("Space")) {
    shootFromTank(player);
  }
}

function updateEnemies(deltaMs) {
  enemies.forEach(enemy => {
    if (!enemy.alive) {
      return;
    }

    enemy.shootCooldown = Math.max(0, enemy.shootCooldown - deltaMs);
    enemy.moveCooldown = Math.max(0, enemy.moveCooldown - deltaMs);

    if (enemy.moveCooldown <= 0) {
      if (Math.random() < 0.35) {
        enemy.dir = Math.floor(Math.random() * 4);
      }

      if (!tryMoveTank(enemy)) {
        enemy.dir = Math.floor(Math.random() * 4);
        tryMoveTank(enemy);
      }

      enemy.moveCooldown = ENEMY_MOVE_INTERVAL_MS + Math.random() * 200;
    }

    if (enemy.shootCooldown <= 0 && Math.random() < ENEMY_SHOOT_CHANCE) {
      shootFromTank(enemy);
    }
  });
}

function checkVictory() {
  if (gameOver || !player.alive) {
    return;
  }

  if (enemies.every(enemy => !enemy.alive)) {
    won = true;
    gameOver = true;
    setStatus("Победа!");
  }
}

function updateGame(deltaMs) {
  if (gameOver) {
    return;
  }

  updatePlayer(deltaMs);
  updateEnemies(deltaMs);
  updateBullets(deltaMs);
  checkVictory();
}

function drawCell(x, y, type) {
  const px = x * TILE_SIZE;
  const py = y * TILE_SIZE;

  if (type === CELL_BRICK) {
    context.fillStyle = "#8b4513";
    context.fillRect(px + 1, py + 1, TILE_SIZE - 2, TILE_SIZE - 2);
    context.fillStyle = "#a0522d";
    context.fillRect(px + 3, py + 3, TILE_SIZE - 8, TILE_SIZE - 8);
    return;
  }

  if (type === CELL_STEEL) {
    context.fillStyle = "#808080";
    context.fillRect(px + 1, py + 1, TILE_SIZE - 2, TILE_SIZE - 2);
    context.fillStyle = "#c0c0c0";
    context.fillRect(px + 4, py + 4, TILE_SIZE - 8, TILE_SIZE - 8);
  }
}

function drawTank(tank, color, barrelColor) {
  if (!tank.alive) {
    return;
  }

  const px = tank.x * TILE_SIZE;
  const py = tank.y * TILE_SIZE;
  const centerX = px + TILE_SIZE / 2;
  const centerY = py + TILE_SIZE / 2;

  context.fillStyle = color;
  context.fillRect(px + 3, py + 3, TILE_SIZE - 6, TILE_SIZE - 6);

  context.fillStyle = barrelColor;
  const direction = DIRECTIONS[tank.dir];
  context.fillRect(
    centerX + direction.dx * 5 - 2,
    centerY + direction.dy * 5 - 2,
    4 + Math.abs(direction.dx) * 4,
    4 + Math.abs(direction.dy) * 4
  );
}

function drawBullet(bullet) {
  const px = bullet.x * TILE_SIZE;
  const py = bullet.y * TILE_SIZE;
  context.fillStyle = "#ffffff";
  context.fillRect(px - 2, py - 2, 4, 4);
}

function render() {
  context.fillStyle = "#000000";
  context.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

  for (let y = 0; y < GRID_SIZE; y += 1) {
    for (let x = 0; x < GRID_SIZE; x += 1) {
      const cell = walls[y][x];
      if (cell !== CELL_EMPTY) {
        drawCell(x, y, cell);
      }
    }
  }

  drawTank(player, "#ffd020", "#c0a000");
  enemies.forEach(enemy => drawTank(enemy, "#d02020", "#801010"));
  bullets.forEach(drawBullet);
}

function gameLoop(timestamp) {
  if (pageElement?.classList.contains("hidden")) {
    lastTimestamp = timestamp;
    animationId = window.requestAnimationFrame(gameLoop);
    return;
  }

  const deltaMs = lastTimestamp === 0 ? 16 : Math.min(timestamp - lastTimestamp, 50);
  lastTimestamp = timestamp;

  updateGame(deltaMs);
  render();

  animationId = window.requestAnimationFrame(gameLoop);
}

function handleKeyDown(event) {
  if (event.code === "Space") {
    event.preventDefault();
  }
  if (event.code in keyDirection || event.code === "Space") {
    pressedKeys.add(event.code);
  }
}

function handleKeyUp(event) {
  pressedKeys.delete(event.code);
}

export function initTanks() {
  canvas = document.getElementById("tanks-canvas");
  pageElement = document.getElementById("page-tanks");
  const resetButton = document.getElementById("tanks-reset");
  if (!canvas || !pageElement || !resetButton) {
    return;
  }

  context = canvas.getContext("2d");
  resetButton.addEventListener("click", resetGame);
  window.addEventListener("keydown", handleKeyDown);
  window.addEventListener("keyup", handleKeyUp);

  resetGame();
  lastTimestamp = 0;
  if (animationId !== null) {
    window.cancelAnimationFrame(animationId);
  }
  animationId = window.requestAnimationFrame(gameLoop);
}
