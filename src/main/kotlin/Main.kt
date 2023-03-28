import java.math.BigDecimal
import java.util.*
import kotlin.math.pow

fun main() {
    // Declaring an empty Map that will store variables
    val variables: MutableMap<String, BigDecimal> = mutableMapOf()

    while (true) {
        val rawInput = readln().trim().replace("\\s+".toRegex()," ")
        when (checkInput(rawInput, variables)) {
            State.EMPTY -> continue
            State.EXIT -> break
            State.HELP -> println("This is a Smart Calculator that can evaluate any mathematical expression")
            State.UNKNOWN_COMMAND -> println("Unknown command")
            State.UNKNOWN_VARIABLE -> println("Unknown variable")
            State.INVALID_IDENTIFIER -> println("Invalid identifier")
            State.INVALID_ASSIGNMENT -> println("Invalid assignment")
            State.INVALID_EXPRESSION -> println("Invalid expression")
            State.VALID_INPUT -> getResult(rawInput, variables)
        }
    }
    println("Bye!")
}

fun checkInput(input: String, varsArg: MutableMap<String, BigDecimal>): State {
    return when {
        input == "" -> State.EMPTY
        input == "/exit" -> State.EXIT
        input == "/help" -> State.HELP
        isUnknownCommand(input) -> State.UNKNOWN_COMMAND
        isUnknownVariable(input, varsArg) -> State.UNKNOWN_VARIABLE
        isInvalidIdentifier(input) -> State.INVALID_IDENTIFIER
        isInvalidAssignment(input) -> State.INVALID_ASSIGNMENT
        isInvalidExpression(input) -> State.INVALID_EXPRESSION
        else -> State.VALID_INPUT
    }
}

fun isUnknownCommand(input: String): Boolean {
    // Checking if input:
    // 1 - starts with "/"
    // 2 - is not "/exit" or "/help"
    return Regex("^/.*").matches(input) && !Regex("/exit|/help").matches(input)
}

fun isUnknownVariable(rawInputArg: String, varsArg: MutableMap<String, BigDecimal>): Boolean {
    // Checking if input:
    // 1 - contains "=", which means we are in a case of assignment
    //     1.1 - we split the input to [left] and [right]
    //     1.2 - if [right] is a Valid variable, then we check if it is stored before
    // 2 - else, we split the input by white spaces
    //     2.1 - we check if everything that is a Valid variable is stored before
    return if (rawInputArg.contains("=")) {
        val input = rawInputArg.split(Regex("\\s*=\\s*"))
        if (isValidVar(input[1]))  !varsArg.containsKey(input[1])
        else false
    } else {
        val input = rawInputArg.split(Regex("\\s+"))
        var contains = true
        for (i in input) {
            if (isValidVar(i) && !varsArg.containsKey(i)) {
                contains = false
                break
            }
        }
        !contains
    }
}

fun isInvalidIdentifier(input: String): Boolean {
    // Checking if input contains "=", which means we are in a case of assignment
    // 1 - we split the input to [left] and [right]
    // 2 - we check if [left] is a Valid variable
    return if (input.contains("=")) {
        val vars = input.split(Regex("\\s*=\\s*"))
        !isValidVar(vars[0])
    } else false
}

fun isInvalidAssignment(input: String): Boolean {
    // Checking if input contains "=", which means we are in a case of assignment
    // 1 - we split the input to [left] and [right], with a limit of 2, to put everything after the first "=" in [right]
    // 2 - we check if [right] is not a Number, then it is a Valid variable
    return if (input.contains("=")) {
        val vars = input.split(Regex("\\s*=\\s*"), 2)
        if (!"[0-9-]+".toRegex().matches(vars[1])) !isValidVar(vars.last())
        else false
    } else false
}

fun isInvalidExpression(input: String): Boolean {
    // 1 - We check if input is valid
    val invalidInput = !"[a-zA-Z\\d\\s()\\-+*^=/]+".toRegex().matches(input)

    // 2 - We check if all opening brackets has a left to close it
    val stack = Stack<Char>()
    for (char in input) {
        when (char) {
            '(' -> stack.push(char)
            ')' -> if (stack.isNotEmpty()) {
                if (stack.peek() == '(') stack.pop()
            } else stack.push(')')
        }
    }

    // 3 - We check if there is only oe occurrence of [*/^]
    val list = input.toList()
    var invalidDuplicate = false
    for (i in list.indices) {
        if (i == list.lastIndex) continue
        else if ("[*/^]".toRegex().matches(list[i].toString())) {
            invalidDuplicate = "[*/^]".toRegex().matches(list[i+1].toString())
        }
        if (invalidDuplicate) break
    }

    return invalidInput || !stack.isEmpty() || invalidDuplicate
}

fun getResult(rawInputArg: String, varsArg: MutableMap<String, BigDecimal>) {
    // 1 - if input contains "=", we call [fun assign] to check if we can do a successful assignment
    // 2 - else, if all the input is a Valid variable, then we fetch its value
    // 3 - else, we call [fun calculate] to start calculating
    if (rawInputArg.contains("=")) assign(rawInputArg, varsArg)
    else if (isValidVar(rawInputArg)) println(varsArg[rawInputArg])
    else calculate(rawInputArg, varsArg)
}

fun assign(rawInputArg: String, varsArg: MutableMap<String, BigDecimal>) {
    // We split the input by "=", then
    // 1 - if [Right] is a numeric value, then we store it
    // 2 - else, we store the fetched value of the [Right] variable
    val input = rawInputArg.split(Regex("\\s*=\\s*"))
    if ("[0-9-]*".toRegex().matches(input[1])) varsArg[input[0]] = input[1].toBigDecimal()
    else varsArg[input[0]] = varsArg.getValue(input[1])
}

fun calculate(rawInputArg: String, varsArg: MutableMap<String, BigDecimal>) {
    // 1 - We simplify the input to remove sequences of '+' and '-'
    // 2 - We convert the simplified input to a Postfix Notation
    val postfix = infixToPostfix(simplifyExpression(rawInputArg))

    // 3 - We Calculate the postfix notation
    println(evaluatePostfix(postfix, varsArg))
}

fun simplifyExpression(expression: String): String {
    var simplifiedExpression = expression
    while ("++" in simplifiedExpression || "+-" in simplifiedExpression || "-+" in simplifiedExpression || "--" in simplifiedExpression) {
        simplifiedExpression = simplifiedExpression.replace("++", "+")
        simplifiedExpression = simplifiedExpression.replace("+-", "-")
        simplifiedExpression = simplifiedExpression.replace("-+", "-")
        simplifiedExpression = simplifiedExpression.replace("--", "+")
    }
    return simplifiedExpression
}

fun infixToPostfix(expression: String): String {
    val stack = Stack<Char>()
    var postfix = ""
    var isNegative = false

    for (i in expression.indices) {
        val char = expression[i]
        when {
            char == ' ' -> postfix += ' '
            char.isLetterOrDigit() -> {
                if (char.isDigit()) {
                    if (isNegative) {
                        postfix += '-'
                        isNegative = false
                    }
                    postfix += char
                } else postfix += char
            }
            char == '-' -> {
                if (postfix.isEmpty()
                    || expression[i + 1].isLetterOrDigit()) isNegative = true
                else {
                    while (stack.isNotEmpty() && precedence(char) <= precedence(stack.peek())) {
                        postfix += ' '
                        postfix += stack.pop()
                    }
                    stack.push(char)
                }
            }
            char == '(' -> stack.push(char)
            char == ')' -> {
                while (stack.isNotEmpty() && stack.peek() != '(') {
                    postfix += ' '
                    postfix += stack.pop()
                }
                stack.pop()
            }
            else -> {
                while (stack.isNotEmpty() && precedence(char) <= precedence(stack.peek())) {
                    postfix += ' '
                    postfix += stack.pop()
                }
                stack.push(char)
            }
        }
    }
    while (stack.isNotEmpty()) {
        postfix += ' '
        postfix += stack.pop()
    }
    return postfix.replace("\\s+".toRegex()," ")
}

fun precedence(char: Char): Int {
    return when (char) {
        '+', '-' -> 1
        '*', '/' -> 2
        '^' -> 3
        else -> -1
    }
}

fun evaluatePostfix(expression: String, varsArg: MutableMap<String, BigDecimal>): BigDecimal {
    val stack = Stack<BigDecimal>()

    for (token in expression.split(" ")) {
        when {
            token.toBigDecimalOrNull() != null -> stack.push(token.toBigDecimal())
            isValidVar(token) -> stack.push(varsArg.getValue(token))
            token == "+" -> {
                val operand2 = stack.pop()
                val operand1 = stack.pop()
                stack.push(operand1 + operand2)
            }
            token == "-" -> {
                val operand2 = stack.pop()
                val operand1 = stack.pop()
                stack.push(operand1 - operand2)
            }
            token == "*" -> {
                val operand2 = stack.pop()
                val operand1 = stack.pop()
                stack.push(operand1 * operand2)
            }
            token == "/" -> {
                val operand2 = stack.pop()
                val operand1 = stack.pop()
                stack.push(operand1 / operand2)
            }
            token == "^" -> {
                val operand2 = stack.pop().toDouble()
                val operand1 = stack.pop().toDouble()
                stack.push(operand1.pow(operand2).toBigDecimal())
            }
        }
    }
    return stack.pop()
}

// A function to check if an input is a sequence of Latin letters
fun isValidVar(rawInputArg: String) = "[a-zA-Z]+".toRegex().matches(rawInputArg)

enum class State {
    EMPTY,
    EXIT,
    HELP,
    UNKNOWN_COMMAND,
    UNKNOWN_VARIABLE,
    INVALID_IDENTIFIER,
    INVALID_ASSIGNMENT,
    INVALID_EXPRESSION,
    VALID_INPUT
}