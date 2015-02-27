goog.addDependency("base.js", ['goog'], []);
goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.object', 'goog.string.StringBuffer', 'goog.array']);
goog.addDependency("../utils/utils.js", ['utils.utils'], ['cljs.core']);
goog.addDependency("../cljs/reader.js", ['cljs.reader'], ['goog.string', 'cljs.core', 'goog.string.StringBuffer']);
goog.addDependency("../tailrecursion/priority_map.js", ['tailrecursion.priority_map'], ['cljs.core', 'cljs.reader']);
goog.addDependency("../tailrecursion/javelin.js", ['tailrecursion.javelin'], ['cljs.core', 'tailrecursion.priority_map']);
goog.addDependency("../core/settings.js", ['core.settings'], ['cljs.core', 'tailrecursion.javelin']);
goog.addDependency("../boot/cljs/main.js", ['boot.cljs.main'], ['utils.utils', 'core.canvas_interface', 'cljs.core', 'utils.dom.dom_utils', 'test.project_tests', 'core.settings', 'utils.dom.dnd_utils']);