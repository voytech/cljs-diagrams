// Compiled by ClojureScript 0.0-2411
goog.provide('utils.utils');
goog.require('cljs.core');
utils.utils.parse_int = (function parse_int(s){
return (new utils.utils.Integer(cljs.core.re_find.call(null,/[0-9]*/,s)));
});
