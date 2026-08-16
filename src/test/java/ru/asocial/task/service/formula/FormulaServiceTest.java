package ru.asocial.task.service.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FormulaServiceTest {

	private final FormulaService formulaService = new FormulaService();

	@Test
	void evaluatesSimpleExpression() {
		FormulaEvaluationResult result = formulaService.evaluate("2 + 3 * (4 - 1)");

		assertEquals("2 + 3 * (4 - 1)", result.original());
		assertEquals("2 + 3 * ( 4 - 1 )", result.parsed());
		assertEquals("11", result.result());
	}

	@Test
	void rejectsInvalidExpression() {
		FormulaParseException exception = assertThrows(
				FormulaParseException.class,
				() -> formulaService.evaluate("2 + * 3"));

		assertEquals("Некорректное выражение около: *", exception.getMessage());
	}

	@Test
	void rejectsDivisionByZero() {
		assertThrows(ArithmeticException.class, () -> formulaService.evaluate("1 / 0"));
	}

	@Test
	void evaluatesDecimalLogarithm() {
		FormulaEvaluationResult result = formulaService.evaluate("LG(100)");

		assertEquals("2", result.result());
		assertEquals("LG ( 100 )", result.parsed());
	}

	@Test
	void evaluatesNaturalLogarithm() {
		FormulaEvaluationResult result = formulaService.evaluate("LN(1)");

		assertEquals("0", result.result());
	}

	@Test
	void evaluatesCustomBaseLogarithm() {
		FormulaEvaluationResult result = formulaService.evaluate("LOG2(8)");

		assertEquals("3", result.result());
	}

	@Test
	void evaluatesCustomBaseLogarithmWithoutParentheses() {
		FormulaEvaluationResult result = formulaService.evaluate("LOG10 1000");

		assertEquals("3", result.result());
	}

	@Test
	void rejectsUnknownFunction() {
		assertThrows(FormulaParseException.class, () -> formulaService.evaluate("LOG(100)"));
	}
}
