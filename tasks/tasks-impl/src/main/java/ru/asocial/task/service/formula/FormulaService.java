package ru.asocial.task.service.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class FormulaService {

	public FormulaEvaluationResult evaluate(String formula) {
		if (formula == null || formula.isBlank()) {
			throw new FormulaParseException("Формула не задана");
		}

		String original = formula.trim();
		ExpressionParser parser = new ExpressionParser(original);
		ParseResult parseResult = parser.parse();
		double value = parseResult.value();

		String parsed = parseResult.tokens().stream()
				.map(Token::display)
				.collect(Collectors.joining(" "));

		String result = formatResult(value);
		return new FormulaEvaluationResult(original, parsed, result);
	}

	private String formatResult(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			throw new ArithmeticException("Результат вычисления некорректен: " + value);
		}

		if (Math.abs(value - Math.rint(value)) < 1e-9) {
			return String.valueOf((long) Math.rint(value));
		}

		return String.format(Locale.US, "%.10g", value);
	}

	private enum TokenType {
		NUMBER,
		FUNCTION,
		PLUS,
		MINUS,
		MULTIPLY,
		DIVIDE,
		LPAREN,
		RPAREN,
		EOF
	}

	private record Token(TokenType type, String lexeme) {

		String display() {
			return switch (type) {
				case NUMBER -> lexeme;
				case FUNCTION -> lexeme;
				case PLUS -> "+";
				case MINUS -> "-";
				case MULTIPLY -> "*";
				case DIVIDE -> "/";
				case LPAREN -> "(";
				case RPAREN -> ")";
				case EOF -> "";
			};
		}
	}

	private record ParseResult(double value, List<Token> tokens) {
	}

	private static final class ExpressionParser {

		private final String input;
		private int position;
		private Token currentToken;
		private final List<Token> tokens = new ArrayList<>();

		private ExpressionParser(String input) {
			this.input = input;
			this.currentToken = readNextToken();
		}

		private ParseResult parse() {
			double value = parseExpression();
			if (currentToken.type() != TokenType.EOF) {
				throw new FormulaParseException("Неожиданный символ: " + currentToken.lexeme());
			}
			return new ParseResult(value, List.copyOf(tokens));
		}

		private double parseExpression() {
			double value = parseTerm();

			while (currentToken.type() == TokenType.PLUS || currentToken.type() == TokenType.MINUS) {
				Token operator = currentToken;
				consume(operator.type());
				double right = parseTerm();
				value = operator.type() == TokenType.PLUS ? value + right : value - right;
			}

			return value;
		}

		private double parseTerm() {
			double value = parseFactor();

			while (currentToken.type() == TokenType.MULTIPLY || currentToken.type() == TokenType.DIVIDE) {
				Token operator = currentToken;
				consume(operator.type());
				double right = parseFactor();
				if (operator.type() == TokenType.MULTIPLY) {
					value *= right;
				}
				else {
					if (right == 0.0d) {
						throw new ArithmeticException("Деление на ноль");
					}
					value /= right;
				}
			}

			return value;
		}

		private double parseFactor() {
			if (currentToken.type() == TokenType.MINUS) {
				consume(TokenType.MINUS);
				return -parseFactor();
			}

			if (currentToken.type() == TokenType.PLUS) {
				consume(TokenType.PLUS);
				return parseFactor();
			}

			if (currentToken.type() == TokenType.FUNCTION) {
				String functionName = currentToken.lexeme();
				consume(TokenType.FUNCTION);
				double argument = parseFunctionArgument();
				return applyFunction(functionName, argument);
			}

			if (currentToken.type() == TokenType.NUMBER) {
				double value = Double.parseDouble(currentToken.lexeme());
				consume(TokenType.NUMBER);
				return value;
			}

			if (currentToken.type() == TokenType.LPAREN) {
				consume(TokenType.LPAREN);
				double value = parseExpression();
				if (currentToken.type() != TokenType.RPAREN) {
					throw new FormulaParseException("Ожидалась закрывающая скобка");
				}
				consume(TokenType.RPAREN);
				return value;
			}

			throw new FormulaParseException("Некорректное выражение около: " + currentToken.lexeme());
		}

		private double parseFunctionArgument() {
			if (currentToken.type() == TokenType.LPAREN) {
				consume(TokenType.LPAREN);
				double value = parseExpression();
				if (currentToken.type() != TokenType.RPAREN) {
					throw new FormulaParseException("Ожидалась закрывающая скобка");
				}
				consume(TokenType.RPAREN);
				return value;
			}

			return parseFactor();
		}

		private double applyFunction(String functionName, double argument) {
			if (argument <= 0.0d) {
				throw new ArithmeticException("Логарифм определён только для положительных чисел");
			}

			return switch (functionName) {
				case "LG" -> Math.log10(argument);
				case "LN" -> Math.log(argument);
				default -> applyCustomLog(functionName, argument);
			};
		}

		private double applyCustomLog(String functionName, double argument) {
			if (!functionName.startsWith("LOG") || functionName.length() <= 3) {
				throw new FormulaParseException("Неизвестная функция: " + functionName);
			}

			String basePart = functionName.substring(3);
			if (basePart.isEmpty() || !basePart.chars().allMatch(Character::isDigit)) {
				throw new FormulaParseException("Некорректное основание логарифма: " + functionName);
			}

			double base = Double.parseDouble(basePart);
			if (base <= 0.0d || base == 1.0d) {
				throw new ArithmeticException("Недопустимое основание логарифма: " + basePart);
			}

			return Math.log(argument) / Math.log(base);
		}

		private void consume(TokenType expectedType) {
			if (currentToken.type() != expectedType) {
				throw new FormulaParseException("Ожидался другой элемент выражения");
			}

			if (currentToken.type() != TokenType.EOF) {
				tokens.add(currentToken);
			}

			currentToken = readNextToken();
		}

		private Token readNextToken() {
			skipWhitespace();

			if (position >= input.length()) {
				return new Token(TokenType.EOF, "");
			}

			char character = input.charAt(position);

			return switch (character) {
				case '+' -> advanceSingle(TokenType.PLUS, "+");
				case '-' -> advanceSingle(TokenType.MINUS, "-");
				case '*' -> advanceSingle(TokenType.MULTIPLY, "*");
				case '/' -> advanceSingle(TokenType.DIVIDE, "/");
				case '(' -> advanceSingle(TokenType.LPAREN, "(");
				case ')' -> advanceSingle(TokenType.RPAREN, ")");
				default -> {
					if (Character.isDigit(character) || character == '.') {
						yield readNumber();
					}
					if (Character.isLetter(character)) {
						yield readFunction();
					}
					throw new FormulaParseException("Недопустимый символ: " + character);
				}
			};
		}

		private Token readFunction() {
			int start = position;

			while (position < input.length()) {
				char character = input.charAt(position);
				if (!Character.isLetterOrDigit(character)) {
					break;
				}
				position++;
			}

			String functionName = input.substring(start, position).toUpperCase(Locale.ROOT);

			if (functionName.equals("LG") || functionName.equals("LN")) {
				return new Token(TokenType.FUNCTION, functionName);
			}

			if (functionName.startsWith("LOG") && functionName.length() > 3) {
				String basePart = functionName.substring(3);
				if (basePart.chars().allMatch(Character::isDigit)) {
					return new Token(TokenType.FUNCTION, functionName);
				}
			}

			throw new FormulaParseException("Неизвестная функция: " + functionName);
		}

		private Token advanceSingle(TokenType type, String lexeme) {
			position++;
			return new Token(type, lexeme);
		}

		private Token readNumber() {
			int start = position;

			while (position < input.length()) {
				char character = input.charAt(position);
				if (!Character.isDigit(character) && character != '.') {
					break;
				}
				position++;
			}

			String lexeme = input.substring(start, position);
			if (lexeme.chars().filter(ch -> ch == '.').count() > 1) {
				throw new FormulaParseException("Некорректное число: " + lexeme);
			}

			return new Token(TokenType.NUMBER, lexeme);
		}

		private void skipWhitespace() {
			while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
				position++;
			}
		}
	}
}
