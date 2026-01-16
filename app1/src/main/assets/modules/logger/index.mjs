/**
 * Common logging utilities
 * Pre-bundled in Android assets
 */
export function logInfo(message) {
    console.info("[INFO]", message);
}

export function logError(message) {
    console.error("[ERROR]", message);
}

export function logSuccess(message) {
    console.log("[SUCCESS]", message);
}

export function logWarning(message) {
    console.warn("[WARN]", message);
}

export function logDebug(message) {
    console.log("[DEBUG]", message);
}

