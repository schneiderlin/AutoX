(ns squint-compiler.api
  (:require [clojure.string :as str]))


(defn query-handler [query]
  (case (:query/kind query)
    
    :query/scripts
    [{:name "Phase 1 - Console Test"
      :code "console.log(\"Hello from JavaScript!\");
console.info(\"This is an info message\");
console.warn(\"This is a warning message\");
console.error(\"This is an error message\");
console.log(\"Phase 1 test complete!\");"}
     {:name "Phase 4 - Common Modules Test"
      :code "// Import pre-bundled common modules from Android assets
// Uses Node.js-like module resolution: package name resolves to modules/package/index.mjs
import { formatMessage, add, multiply, VERSION } from 'utils';
import { logInfo, logSuccess, logError, logWarning } from 'logger';

console.log(\"Testing pre-bundled common modules...\");
console.log(\"Utils version:\", VERSION);

const msg = formatMessage(\"Hello from common module!\");
logInfo(msg);

const sum = add(5, 3);
logSuccess(`5 + 3 = ${sum}`);

const product = multiply(4, 7);
logSuccess(`4 * 7 = ${product}`);

logWarning(\"This is a test warning message\");
logError(\"This is a test error message\");

console.log(\"Common modules test complete ✓\");"}
     {:name "Phase 5 - Squint-CLJS Test"
      :code "// Test squint-cljs runtime with Node.js built-ins
import * as squint_core from 'squint-cljs/core.js';
var foo = function (p__1) {
const map__12 = p__1;
const a3 = squint_core.get(map__12, \"a\");
const b4 = squint_core.get(map__12, \"b\");
const c5 = squint_core.get(map__12, \"c\");
return (a3 + b4 + c5);

};
squint_core.println(foo(({\"a\": 1, \"b\": 2, \"c\": 3})));

export { foo }
"}]
    
    nil))

(comment
  (query-handler {:query/kind :query/scripts, :query/data {:page 1}})
  :rcf)

(defn command-handler [command]
  (println "command:" command)
  (case (:command/kind command)
    :command/save-layout
    (let [{:keys [layout-data timestamp]} (:command/data command)
          filename (str "layouts/layout_" (str/replace (str timestamp) ":" "-") ".json")
          _ (.mkdirs (java.io.File. (java.io.File. filename) ".."))
          _ (spit filename layout-data)]
      {:status "ok" :file filename})

    nil))
