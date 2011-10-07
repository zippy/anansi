goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.string.StringBuffer', 'goog.object', 'goog.array']);
goog.addDependency("../cljs/reader.js", ['cljs.reader'], ['cljs.core', 'goog.string']);
goog.addDependency("../clojure/browser/dom.js", ['clojure.browser.dom'], ['cljs.core', 'goog.dom']);
goog.addDependency("../clojure/string.js", ['clojure.string'], ['cljs.core', 'goog.string', 'goog.string.StringBuffer']);
goog.addDependency("../utils.js", ['test.utils'], ['cljs.core']);
goog.addDependency("../dom-helpers.js", ['test.dom_helpers'], ['cljs.core', 'clojure.string', 'goog.dom']);
goog.addDependency("../makezip.js", ['test.makezip'], ['cljs.core', 'test.dom_helpers', 'goog.ui.Zippy']);
goog.addDependency("../debug.js", ['test.debug'], ['cljs.core', 'test.dom_helpers']);
goog.addDependency("../core.js", ['test.core'], ['cljs.core', 'test.dom_helpers', 'goog.ui.Dialog', 'test.utils', 'goog.events', 'cljs.reader', 'clojure.browser.dom', 'clojure.string', 'test.makezip', 'goog.editor.Field', 'goog.style', 'goog.ui.Prompt', 'goog.ui.Button', 'goog.Uri', 'goog.ui.LabelInput', 'goog.net.XhrIo', 'test.debug', 'goog.debug.DebugWindow']);
