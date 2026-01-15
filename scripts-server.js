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
    {
        name: "Phase 2 - Single Click",
        code: `console.log("Testing automation from JavaScript...");
console.log("Clicking at coordinates (500, 500)...");
var result = auto.click(500, 500);
console.log("Click result: " + result);
if (result) {
    console.log("SUCCESS ✓");
} else {
    console.log("FAILED ✗");
}`
    },
    {
        name: "Phase 2 - Multiple Clicks",
        code: `console.log("Performing multiple clicks from JavaScript...");

console.log("Click 1 at (100, 100)...");
var result1 = auto.click(100, 100);
console.log("Click 1 result: " + result1);

console.log("Click 2 at (200, 200)...");
var result2 = auto.click(200, 200);
console.log("Click 2 result: " + result2);

console.log("Long click at (300, 300)...");
var result3 = auto.longClick(300, 300);
console.log("Long click result: " + result3);

console.log("Multiple clicks test complete ✓");`
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

