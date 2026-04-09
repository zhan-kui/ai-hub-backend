package com.aihub.agent.skill;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 计算器工具（Skill）示例。
 * <p>
 * 支持四则运算和括号，内部实现步骤：
 * <p>1) tokenize：把表达式拆分为 token</p>
 * <p>2) toRpn：中缀转后缀（逆波兰）</p>
 * <p>3) eval：后缀表达式求值</p>
 */
@Component
public class CalculatorSkill {

    /**
     * 计算数学表达式。
     *
     * @param expression 表达式（支持 + - * / 与括号）
     * @return 结果文本
     */
    @Tool("计算数学表达式，支持 + - * / 与括号")
    public String calculate(@P("数学表达式") String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("表达式不能为空");
        }

        BigDecimal result = eval(expression.replaceAll("\\s+", ""));
        BigDecimal normalized = result.stripTrailingZeros();
        return expression + " = " + normalized.toPlainString();
    }

    /**
     * 执行表达式求值。
     */
    private BigDecimal eval(String expr) {
        List<String> rpn = toRpn(tokenize(expr));
        Deque<BigDecimal> stack = new ArrayDeque<>();

        for (String token : rpn) {
            if (isNumber(token)) {
                stack.push(new BigDecimal(token));
                continue;
            }

            BigDecimal b = stack.pop();
            BigDecimal a = stack.pop();
            BigDecimal v;
            switch (token) {
                case "+" -> v = a.add(b);
                case "-" -> v = a.subtract(b);
                case "*" -> v = a.multiply(b);
                case "/" -> {
                    if (b.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("除数不能为 0");
                    }
                    v = a.divide(b, 10, RoundingMode.HALF_UP);
                }
                default -> throw new IllegalArgumentException("非法操作符: " + token);
            }
            stack.push(v);
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("表达式不合法");
        }
        return stack.pop();
    }

    /**
     * 把表达式拆分为 token 列表。
     */
    private List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                int j = i + 1;
                while (j < expr.length()) {
                    char n = expr.charAt(j);
                    if (Character.isDigit(n) || n == '.') {
                        j++;
                    } else {
                        break;
                    }
                }
                tokens.add(expr.substring(i, j));
                i = j;
                continue;
            }

            if (isOperatorChar(c) || c == '(' || c == ')') {
                if (c == '-' && (tokens.isEmpty() || isOperator(tokens.get(tokens.size() - 1))
                        || "(".equals(tokens.get(tokens.size() - 1)))) {
                    int j = i + 1;
                    while (j < expr.length()) {
                        char n = expr.charAt(j);
                        if (Character.isDigit(n) || n == '.') {
                            j++;
                        } else {
                            break;
                        }
                    }
                    if (j == i + 1) {
                        throw new IllegalArgumentException("表达式不合法");
                    }
                    tokens.add(expr.substring(i, j));
                    i = j;
                    continue;
                }

                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            throw new IllegalArgumentException("包含非法字符: " + c);
        }
        return tokens;
    }

    /**
     * 中缀表达式转后缀表达式（RPN）。
     */
    private List<String> toRpn(List<String> tokens) {
        List<String> out = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();

        for (String token : tokens) {
            if (isNumber(token)) {
                out.add(token);
                continue;
            }

            if (isOperator(token)) {
                while (!ops.isEmpty() && isOperator(ops.peek())
                        && priority(ops.peek()) >= priority(token)) {
                    out.add(ops.pop());
                }
                ops.push(token);
                continue;
            }

            if ("(".equals(token)) {
                ops.push(token);
                continue;
            }

            if (")".equals(token)) {
                while (!ops.isEmpty() && !"(".equals(ops.peek())) {
                    out.add(ops.pop());
                }
                if (ops.isEmpty() || !"(".equals(ops.pop())) {
                    throw new IllegalArgumentException("括号不匹配");
                }
                continue;
            }

            throw new IllegalArgumentException("非法 token: " + token);
        }

        while (!ops.isEmpty()) {
            String op = ops.pop();
            if ("(".equals(op) || ")".equals(op)) {
                throw new IllegalArgumentException("括号不匹配");
            }
            out.add(op);
        }

        return out;
    }

    /**
     * 判断是否为操作符字符。
     */
    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    /**
     * 判断 token 是否为操作符。
     */
    private boolean isOperator(String token) {
        return "+".equals(token) || "-".equals(token) || "*".equals(token) || "/".equals(token);
    }

    /**
     * 判断 token 是否为数字。
     */
    private boolean isNumber(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (".".equals(token) || "-.".equals(token)) {
            return false;
        }
        try {
            new BigDecimal(token);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 操作符优先级。
     */
    private int priority(String op) {
        if ("*".equals(op) || "/".equals(op)) {
            return 2;
        }
        if ("+".equals(op) || "-".equals(op)) {
            return 1;
        }
        return 0;
    }
}