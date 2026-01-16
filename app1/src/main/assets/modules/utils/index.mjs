/**
 * Common utility functions
 * Pre-bundled in Android assets
 */
export function formatMessage(msg) {
    return "[UTILS] " + msg;
}

export function add(a, b) {
    return a + b;
}

export function multiply(a, b) {
    return a * b;
}

export function subtract(a, b) {
    return a - b;
}

export function divide(a, b) {
    if (b === 0) {
        throw new Error("Division by zero");
    }
    return a / b;
}

export const VERSION = "1.0.0";

