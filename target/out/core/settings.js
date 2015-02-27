// Compiled by ClojureScript 0.0-2411
goog.provide('core.settings');
goog.require('cljs.core');
goog.require('tailrecursion.javelin');
goog.require('tailrecursion.javelin');
core.settings.a4k = "A4";
core.settings.a3k = "A3";
core.settings.a5k = "A5";
core.settings.a6k = "A6";
core.settings.tstk = "T";
core.settings.test = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"key","key",-1516042587),core.settings.test,new cljs.core.Keyword(null,"width","width",-384071477),(594),new cljs.core.Keyword(null,"height","height",1025178622),(670),new cljs.core.Keyword(null,"ratio","ratio",-926560044),((594) / (670))], null);
core.settings.a3 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"key","key",-1516042587),core.settings.a3k,new cljs.core.Keyword(null,"width","width",-384071477),(297),new cljs.core.Keyword(null,"height","height",1025178622),(420),new cljs.core.Keyword(null,"ratio","ratio",-926560044),((297) / (420))], null);
core.settings.a4 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"key","key",-1516042587),core.settings.a4k,new cljs.core.Keyword(null,"width","width",-384071477),(210),new cljs.core.Keyword(null,"height","height",1025178622),(297),new cljs.core.Keyword(null,"ratio","ratio",-926560044),((210) / (297))], null);
core.settings.a5 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"key","key",-1516042587),core.settings.a5k,new cljs.core.Keyword(null,"width","width",-384071477),(148),new cljs.core.Keyword(null,"height","height",1025178622),(210),new cljs.core.Keyword(null,"ratio","ratio",-926560044),((148) / (210))], null);
core.settings.a6 = new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"key","key",-1516042587),core.settings.a6k,new cljs.core.Keyword(null,"width","width",-384071477),(105),new cljs.core.Keyword(null,"height","height",1025178622),(148),new cljs.core.Keyword(null,"ratio","ratio",-926560044),((105) / (148))], null);
core.settings.page_formats = new cljs.core.PersistentArrayMap(null, 5, ["T",core.settings.test,"A3",core.settings.a3,"A4",core.settings.a4,"A5",core.settings.a5,"A6",core.settings.a6], null);
core.settings.settings = tailrecursion.javelin.cell.call(null,new cljs.core.PersistentArrayMap(null, 5, [new cljs.core.Keyword(null,"page-format","page-format",-772708570),core.settings.tstk,new cljs.core.Keyword(null,"snapping","snapping",-1068194089),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"enabled","enabled",1195909756),true,new cljs.core.Keyword(null,"visible","visible",-1024216805),true,new cljs.core.Keyword(null,"attract","attract",747255162),(15),new cljs.core.Keyword(null,"interval","interval",1708495417),(50)], null),new cljs.core.Keyword(null,"multi-page","multi-page",1552701758),false,new cljs.core.Keyword(null,"pages","pages",-285406513),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"count","count",2139924085),(4),new cljs.core.Keyword(null,"two-sided","two-sided",1449726498),false], null),new cljs.core.Keyword(null,"zoom","zoom",-1827487038),(1)], null));
/**
* @param {...*} var_args
*/
core.settings.settings_QMARK_ = (function() { 
var settings_QMARK___delegate = function (one,path){
return tailrecursion.javelin.formula.call(null,(function (G__6467,G__6468,G__6466,G__6465,G__6464){
return G__6464.call(null,G__6465,G__6466.call(null,G__6467,G__6468));
})).call(null,path,one,cljs.core.conj,core.settings.settings,cljs.core.get_in);
};
var settings_QMARK_ = function (one,var_args){
var path = null;
if (arguments.length > 1) {
  path = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1),0);
} 
return settings_QMARK___delegate.call(this,one,path);};
settings_QMARK_.cljs$lang$maxFixedArity = 1;
settings_QMARK_.cljs$lang$applyTo = (function (arglist__6469){
var one = cljs.core.first(arglist__6469);
var path = cljs.core.rest(arglist__6469);
return settings_QMARK___delegate(one,path);
});
settings_QMARK_.cljs$core$IFn$_invoke$arity$variadic = settings_QMARK___delegate;
return settings_QMARK_;
})()
;
/**
* @param {...*} var_args
*/
core.settings.settings_BANG_ = (function() { 
var settings_BANG___delegate = function (val,path){
return cljs.core.swap_BANG_.call(null,core.settings.settings,cljs.core.assoc_in,path,val);
};
var settings_BANG_ = function (val,var_args){
var path = null;
if (arguments.length > 1) {
  path = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1),0);
} 
return settings_BANG___delegate.call(this,val,path);};
settings_BANG_.cljs$lang$maxFixedArity = 1;
settings_BANG_.cljs$lang$applyTo = (function (arglist__6470){
var val = cljs.core.first(arglist__6470);
var path = cljs.core.rest(arglist__6470);
return settings_BANG___delegate(val,path);
});
settings_BANG_.cljs$core$IFn$_invoke$arity$variadic = settings_BANG___delegate;
return settings_BANG_;
})()
;
core.settings.page_width = tailrecursion.javelin.formula.call(null,(function (G__6472,G__6471,G__6474,G__6473){
return new cljs.core.Keyword(null,"width","width",-384071477).cljs$core$IFn$_invoke$arity$1(G__6471.call(null,G__6472,G__6473.call(null,G__6474,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"page-format","page-format",-772708570)], null))));
})).call(null,core.settings.page_formats,cljs.core.get,core.settings.settings,cljs.core.get_in);
core.settings.page_height = tailrecursion.javelin.formula.call(null,(function (G__6476,G__6475,G__6478,G__6477){
return new cljs.core.Keyword(null,"height","height",1025178622).cljs$core$IFn$_invoke$arity$1(G__6475.call(null,G__6476,G__6477.call(null,G__6478,new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"page-format","page-format",-772708570)], null))));
})).call(null,core.settings.page_formats,cljs.core.get,core.settings.settings,cljs.core.get_in);
