goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.array', 'goog.object', 'goog.string.StringBuffer']);
goog.addDependency("../cljs/reader.js", ['cljs.reader'], ['cljs.core', 'goog.string']);
goog.addDependency("../clojure/browser/dom.js", ['clojure.browser.dom'], ['cljs.core', 'goog.dom']);
goog.addDependency("../clojure/string.js", ['clojure.string'], ['cljs.core', 'goog.string', 'goog.string.StringBuffer']);
goog.addDependency("../utils.js", ['test.utils'], ['cljs.core']);
goog.addDependency("../dom-helpers.js", ['test.dom_helpers'], ['cljs.core', 'clojure.string', 'goog.dom']);
goog.addDependency("../makezip.js", ['test.makezip'], ['cljs.core', 'test.dom_helpers', 'goog.ui.Zippy']);
goog.addDependency("../debug.js", ['test.debug'], ['cljs.core', 'test.dom_helpers']);
goog.addDependency("../core.js", ['test.core'], ['test.debug', 'cljs.core', 'goog.ui.Prompt', 'clojure.browser.dom', 'test.makezip', 'goog.ui.LabelInput', 'clojure.string', 'goog.ui.Button', 'cljs.reader', 'goog.net.XhrIo', 'goog.ui.Dialog', 'goog.style', 'test.utils', 'goog.events', 'goog.debug.DebugWindow', 'goog.editor.Field', 'goog.Uri', 'test.dom_helpers']);
