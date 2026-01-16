const http = require('http');
const url = require('url');

const PORT = 3000;

// Sample scripts matching the existing test cases
const scripts = [
    {
        name: "Phase 1 - Console Test",
        code: `console.log("Hello from JavaScript!");
console.info("This is an info message");
console.warn("This is a warning message");
console.error("This is an error message");
console.log("Phase 1 test complete!");`
    },
//     {
//         name: "Phase 2 - Single Click",
//         code: `console.log("Testing automation from JavaScript...");
// console.log("Clicking at coordinates (500, 500)...");
// var result = auto.click(500, 500);
// console.log("Click result: " + result);
// if (result) {
//     console.log("SUCCESS ✓");
// } else {
//     console.log("FAILED ✗");
// }`
//     },
//     {
//         name: "Phase 2 - Multiple Clicks",
//         code: `console.log("Performing multiple clicks from JavaScript...");

// console.log("Click 1 at (100, 100)...");
// var result1 = auto.click(100, 100);
// console.log("Click 1 result: " + result1);

// console.log("Click 2 at (200, 200)...");
// var result2 = auto.click(200, 200);
// console.log("Click 2 result: " + result2);

// console.log("Long click at (300, 300)...");
// var result3 = auto.longClick(300, 300);
// console.log("Long click result: " + result3);

// console.log("Multiple clicks test complete ✓");`
//     },
//     {
//         name: "Phase 3 - Multi-Module Test",
//         code: `import { foo } from './module1.mjs';

// console.log("Module2: Calling foo from module1...");
// const result = foo("Hello from module2!");
// console.log("Module2: Result from foo:", result);
// console.log("Multi-module test complete ✓");`,
//         modules: [
//             {
//                 name: "module1.mjs",
//                 code: `export function foo(message) {
//     console.log("Module1: foo called with:", message);
//     return "foo returned: " + message.toUpperCase();
// }`
//             }
//         ]
//     },
    {
        name: "Phase 4 - Common Modules Test",
        code: `// Import pre-bundled common modules from Android assets
// Uses Node.js-like module resolution: package name resolves to modules/package/index.mjs
import { formatMessage, add, multiply, VERSION } from 'utils';
import { logInfo, logSuccess, logError, logWarning } from 'logger';

console.log("Testing pre-bundled common modules...");
console.log("Utils version:", VERSION);

const msg = formatMessage("Hello from common module!");
logInfo(msg);

const sum = add(5, 3);
logSuccess(\`5 + 3 = \${sum}\`);

const product = multiply(4, 7);
logSuccess(\`4 * 7 = \${product}\`);

logWarning("This is a test warning message");
logError("This is a test error message");

console.log("Common modules test complete ✓");`
    },
    {
        name: "Phase 5 - Squint-CLJS Test",
        code: `// Test squint-cljs runtime with Node.js built-ins
import * as squint_core from 'squint-cljs/core.js';
var foo = function (p__1) {
const map__12 = p__1;
const a3 = squint_core.get(map__12, "a");
const b4 = squint_core.get(map__12, "b");
const c5 = squint_core.get(map__12, "c");
return (a3 + b4 + c5);

};
squint_core.println(foo(({"a": 1, "b": 2, "c": 3})));

export { foo }
`
    }
];

const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
    
    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }
    
    if (parsedUrl.pathname === '/scripts' && req.method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(scripts));
    } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Not Found');
    }
});

server.listen(PORT, () => {
    console.log(`Scripts server running on http://localhost:${PORT}`);
    console.log(`Access scripts at http://localhost:${PORT}/scripts`);
});

