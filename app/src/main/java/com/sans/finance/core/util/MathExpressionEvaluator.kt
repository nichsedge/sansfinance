package com.sans.finance.core.util

/**
 * Utility to parse and safely evaluate basic mathematical expressions in the amount input.
 * Supports: +, -, *, /, x, X, ÷ with standard operator precedence.
 */
object MathExpressionEvaluator {

    private val OPERATORS = setOf('+', '-', '*', '/', 'x', 'X', '÷')

    fun hasArithmetic(input: String): Boolean {
        return input.any { it in OPERATORS }
    }

    fun evaluate(input: String): Double? {
        val sanitized = input.trim()
            .replace("x", "*")
            .replace("X", "*")
            .replace("÷", "/")
            .replace(" ", "")
            .replace(",", ".")

        if (sanitized.isBlank()) return null

        // If it's a simple number without operators
        if (!hasArithmetic(sanitized)) {
            return sanitized.toDoubleOrNull()
        }

        return try {
            parseExpression(sanitized)
        } catch (e: Exception) {
            null
        }
    }

    fun evaluateToCents(input: String): Long? {
        val eval = evaluate(input) ?: return null
        if (eval.isNaN() || eval.isInfinite() || eval <= 0.0) return null
        return kotlin.math.round(eval * 100).toLong()
    }

    private fun parseExpression(expression: String): Double {
        val tokens = tokenize(expression)
        if (tokens.isEmpty()) throw IllegalArgumentException("Empty tokens")

        // First pass: perform multiplication and division
        val firstPassTokens = mutableListOf<Any>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token is Char && (token == '*' || token == '/')) {
                if (firstPassTokens.isEmpty() || i + 1 >= tokens.size) {
                    throw IllegalArgumentException("Malformed expression")
                }
                val prevNum = firstPassTokens.removeAt(firstPassTokens.size - 1) as? Double
                    ?: throw IllegalArgumentException("Expected number before $token")
                val nextNum = tokens[i + 1] as? Double
                    ?: throw IllegalArgumentException("Expected number after $token")

                if (token == '/' && nextNum == 0.0) {
                    throw ArithmeticException("Division by zero")
                }

                val result = if (token == '*') prevNum * nextNum else prevNum / nextNum
                firstPassTokens.add(result)
                i += 2
            } else {
                firstPassTokens.add(token)
                i++
            }
        }

        // Second pass: perform addition and subtraction
        if (firstPassTokens.isEmpty()) throw IllegalArgumentException("No tokens after first pass")
        var result = firstPassTokens[0] as? Double
            ?: throw IllegalArgumentException("First token must be a number")

        var j = 1
        while (j < firstPassTokens.size) {
            val op = firstPassTokens[j] as? Char
                ?: throw IllegalArgumentException("Expected operator at index $j")
            val nextVal = firstPassTokens.getOrNull(j + 1) as? Double
                ?: throw IllegalArgumentException("Expected number after operator $op")

            result = when (op) {
                '+' -> result + nextVal
                '-' -> result - nextVal
                else -> throw IllegalArgumentException("Unknown operator $op")
            }
            j += 2
        }

        return result
    }

    private fun tokenize(expr: String): List<Any> {
        val tokens = mutableListOf<Any>()
        var currentNum = StringBuilder()

        for (i in expr.indices) {
            val ch = expr[i]
            if (ch.isDigit() || ch == '.') {
                currentNum.append(ch)
            } else if (ch in setOf('+', '-', '*', '/')) {
                // Check if this '-' is a unary minus at the beginning or after an operator
                if (ch == '-' && currentNum.isEmpty() && (tokens.isEmpty() || tokens.last() is Char)) {
                    currentNum.append(ch)
                } else {
                    if (currentNum.isNotEmpty()) {
                        val num = currentNum.toString().toDoubleOrNull()
                            ?: throw IllegalArgumentException("Invalid number: $currentNum")
                        tokens.add(num)
                        currentNum = StringBuilder()
                    }
                    tokens.add(ch)
                }
            } else {
                throw IllegalArgumentException("Unexpected character: $ch")
            }
        }

        if (currentNum.isNotEmpty()) {
            val num = currentNum.toString().toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid number: $currentNum")
            tokens.add(num)
        }

        return tokens
    }
}
