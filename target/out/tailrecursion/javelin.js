// Compiled by ClojureScript 0.0-2411
goog.provide('tailrecursion.javelin');
goog.require('cljs.core');
goog.require('tailrecursion.priority_map');
goog.require('tailrecursion.priority_map');




tailrecursion.javelin._STAR_tx_STAR_ = null;
tailrecursion.javelin.last_rank = cljs.core.atom.call(null,(0));
/**
* Like tree-seq but traversal is breadth-first instead of depth-first.
*/
tailrecursion.javelin.bf_seq = (function bf_seq(branch_QMARK_,children,root){
var walk = (function walk(queue){
var temp__4126__auto__ = cljs.core.peek.call(null,queue);
if(cljs.core.truth_(temp__4126__auto__)){
var node = temp__4126__auto__;
return (new cljs.core.LazySeq(null,((function (node,temp__4126__auto__){
return (function (){
return cljs.core.cons.call(null,node,walk.call(null,cljs.core.into.call(null,cljs.core.pop.call(null,queue),(cljs.core.truth_(branch_QMARK_.call(null,node))?children.call(null,node):null))));
});})(node,temp__4126__auto__))
,null,null));
} else {
return null;
}
});
return walk.call(null,cljs.core.conj.call(null,cljs.core.PersistentQueue.EMPTY,root));
});
tailrecursion.javelin.propagate_STAR_ = (function propagate_STAR_(pri_map){
while(true){
var temp__4126__auto__ = cljs.core.first.call(null,cljs.core.peek.call(null,pri_map));
if(cljs.core.truth_(temp__4126__auto__)){
var next = temp__4126__auto__;
var popq = cljs.core.pop.call(null,pri_map);
var old = next.prev;
var new$ = (function (){var temp__4124__auto__ = next.thunk;
if(cljs.core.truth_(temp__4124__auto__)){
var f = temp__4124__auto__;
return f.call(null);
} else {
return next.state;
}
})();
var diff_QMARK_ = cljs.core.not_EQ_.call(null,new$,old);
if(diff_QMARK_){
next.prev = new$;

cljs.core._notify_watches.call(null,next,old,new$);
} else {
}

var G__6481 = ((!(diff_QMARK_))?popq:cljs.core.reduce.call(null,((function (pri_map,popq,old,new$,diff_QMARK_,next,temp__4126__auto__){
return (function (p1__6479_SHARP_,p2__6480_SHARP_){
return cljs.core.assoc.call(null,p1__6479_SHARP_,p2__6480_SHARP_,p2__6480_SHARP_.rank);
});})(pri_map,popq,old,new$,diff_QMARK_,next,temp__4126__auto__))
,popq,next.sinks));
pri_map = G__6481;
continue;
} else {
return null;
}
break;
}
});
tailrecursion.javelin.deref_STAR_ = (function deref_STAR_(x){
if(cljs.core.truth_(tailrecursion.javelin.cell_QMARK_.call(null,x))){
return cljs.core.deref.call(null,x);
} else {
return x;
}
});
tailrecursion.javelin.next_rank = (function next_rank(){
return cljs.core.swap_BANG_.call(null,tailrecursion.javelin.last_rank,cljs.core.inc);
});
tailrecursion.javelin.cell__GT_pm = (function cell__GT_pm(c){
return tailrecursion.priority_map.priority_map.call(null,c,c.rank);
});
tailrecursion.javelin.add_sync_BANG_ = (function add_sync_BANG_(c){
return cljs.core.swap_BANG_.call(null,tailrecursion.javelin._STAR_tx_STAR_,cljs.core.assoc,c,c.rank);
});
tailrecursion.javelin.safe_nth = (function safe_nth(c,i){
try{return cljs.core.nth.call(null,c,i);
}catch (e6483){if((e6483 instanceof Error)){
var _ = e6483;
return null;
} else {
throw e6483;

}
}});
tailrecursion.javelin.propagate_BANG_ = (function propagate_BANG_(c){
if(cljs.core.truth_(tailrecursion.javelin._STAR_tx_STAR_)){
var G__6486 = c;
tailrecursion.javelin.add_sync_BANG_.call(null,G__6486);

return G__6486;
} else {
var G__6487 = c;
tailrecursion.javelin.propagate_STAR_.call(null,tailrecursion.javelin.cell__GT_pm.call(null,G__6487));

return G__6487;
}
});
/**
* @param {...*} var_args
*/
tailrecursion.javelin.destroy_cell_BANG_ = (function() { 
var destroy_cell_BANG___delegate = function (this$,p__6488){
var vec__6494 = p__6488;
var keep_watches_QMARK_ = cljs.core.nth.call(null,vec__6494,(0),null);
var srcs = this$.sources;
this$.sources = cljs.core.PersistentVector.EMPTY;

this$.update = null;

this$.thunk = null;

if(cljs.core.truth_(keep_watches_QMARK_)){
} else {
this$.watches = cljs.core.PersistentArrayMap.EMPTY;
}

var seq__6495 = cljs.core.seq.call(null,srcs);
var chunk__6496 = null;
var count__6497 = (0);
var i__6498 = (0);
while(true){
if((i__6498 < count__6497)){
var src = cljs.core._nth.call(null,chunk__6496,i__6498);
if(cljs.core.truth_(tailrecursion.javelin.cell_QMARK_.call(null,src))){
src.sinks = cljs.core.disj.call(null,src.sinks,this$);
} else {
}

var G__6499 = seq__6495;
var G__6500 = chunk__6496;
var G__6501 = count__6497;
var G__6502 = (i__6498 + (1));
seq__6495 = G__6499;
chunk__6496 = G__6500;
count__6497 = G__6501;
i__6498 = G__6502;
continue;
} else {
var temp__4126__auto__ = cljs.core.seq.call(null,seq__6495);
if(temp__4126__auto__){
var seq__6495__$1 = temp__4126__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__6495__$1)){
var c__4175__auto__ = cljs.core.chunk_first.call(null,seq__6495__$1);
var G__6503 = cljs.core.chunk_rest.call(null,seq__6495__$1);
var G__6504 = c__4175__auto__;
var G__6505 = cljs.core.count.call(null,c__4175__auto__);
var G__6506 = (0);
seq__6495 = G__6503;
chunk__6496 = G__6504;
count__6497 = G__6505;
i__6498 = G__6506;
continue;
} else {
var src = cljs.core.first.call(null,seq__6495__$1);
if(cljs.core.truth_(tailrecursion.javelin.cell_QMARK_.call(null,src))){
src.sinks = cljs.core.disj.call(null,src.sinks,this$);
} else {
}

var G__6507 = cljs.core.next.call(null,seq__6495__$1);
var G__6508 = null;
var G__6509 = (0);
var G__6510 = (0);
seq__6495 = G__6507;
chunk__6496 = G__6508;
count__6497 = G__6509;
i__6498 = G__6510;
continue;
}
} else {
return null;
}
}
break;
}
};
var destroy_cell_BANG_ = function (this$,var_args){
var p__6488 = null;
if (arguments.length > 1) {
  p__6488 = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1),0);
} 
return destroy_cell_BANG___delegate.call(this,this$,p__6488);};
destroy_cell_BANG_.cljs$lang$maxFixedArity = 1;
destroy_cell_BANG_.cljs$lang$applyTo = (function (arglist__6511){
var this$ = cljs.core.first(arglist__6511);
var p__6488 = cljs.core.rest(arglist__6511);
return destroy_cell_BANG___delegate(this$,p__6488);
});
destroy_cell_BANG_.cljs$core$IFn$_invoke$arity$variadic = destroy_cell_BANG___delegate;
return destroy_cell_BANG_;
})()
;
/**
* @param {...*} var_args
*/
tailrecursion.javelin.set_formula_BANG_ = (function() { 
var set_formula_BANG___delegate = function (this$,p__6514){
var vec__6528 = p__6514;
var f = cljs.core.nth.call(null,vec__6528,(0),null);
var sources = cljs.core.nth.call(null,vec__6528,(1),null);
tailrecursion.javelin.destroy_cell_BANG_.call(null,this$,true);

if(cljs.core.truth_(f)){
this$.sources = cljs.core.conj.call(null,cljs.core.vec.call(null,sources),f);

var seq__6529_6541 = cljs.core.seq.call(null,this$.sources);
var chunk__6530_6542 = null;
var count__6531_6543 = (0);
var i__6532_6544 = (0);
while(true){
if((i__6532_6544 < count__6531_6543)){
var source_6545 = cljs.core._nth.call(null,chunk__6530_6542,i__6532_6544);
if(cljs.core.truth_(tailrecursion.javelin.cell_QMARK_.call(null,source_6545))){
source_6545.sinks = cljs.core.conj.call(null,source_6545.sinks,this$);

if((source_6545.rank > this$.rank)){
var seq__6533_6546 = cljs.core.seq.call(null,tailrecursion.javelin.bf_seq.call(null,cljs.core.identity,((function (seq__6529_6541,chunk__6530_6542,count__6531_6543,i__6532_6544,source_6545,vec__6528,f,sources){
return (function (p1__6512_SHARP_){
return p1__6512_SHARP_.sinks;
});})(seq__6529_6541,chunk__6530_6542,count__6531_6543,i__6532_6544,source_6545,vec__6528,f,sources))
,source_6545));
var chunk__6534_6547 = null;
var count__6535_6548 = (0);
var i__6536_6549 = (0);
while(true){
if((i__6536_6549 < count__6535_6548)){
var dep_6550 = cljs.core._nth.call(null,chunk__6534_6547,i__6536_6549);
dep_6550.rank = tailrecursion.javelin.next_rank.call(null);

var G__6551 = seq__6533_6546;
var G__6552 = chunk__6534_6547;
var G__6553 = count__6535_6548;
var G__6554 = (i__6536_6549 + (1));
seq__6533_6546 = G__6551;
chunk__6534_6547 = G__6552;
count__6535_6548 = G__6553;
i__6536_6549 = G__6554;
continue;
} else {
var temp__4126__auto___6555 = cljs.core.seq.call(null,seq__6533_6546);
if(temp__4126__auto___6555){
var seq__6533_6556__$1 = temp__4126__auto___6555;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__6533_6556__$1)){
var c__4175__auto___6557 = cljs.core.chunk_first.call(null,seq__6533_6556__$1);
var G__6558 = cljs.core.chunk_rest.call(null,seq__6533_6556__$1);
var G__6559 = c__4175__auto___6557;
var G__6560 = cljs.core.count.call(null,c__4175__auto___6557);
var G__6561 = (0);
seq__6533_6546 = G__6558;
chunk__6534_6547 = G__6559;
count__6535_6548 = G__6560;
i__6536_6549 = G__6561;
continue;
} else {
var dep_6562 = cljs.core.first.call(null,seq__6533_6556__$1);
dep_6562.rank = tailrecursion.javelin.next_rank.call(null);

var G__6563 = cljs.core.next.call(null,seq__6533_6556__$1);
var G__6564 = null;
var G__6565 = (0);
var G__6566 = (0);
seq__6533_6546 = G__6563;
chunk__6534_6547 = G__6564;
count__6535_6548 = G__6565;
i__6536_6549 = G__6566;
continue;
}
} else {
}
}
break;
}
} else {
}
} else {
}

var G__6567 = seq__6529_6541;
var G__6568 = chunk__6530_6542;
var G__6569 = count__6531_6543;
var G__6570 = (i__6532_6544 + (1));
seq__6529_6541 = G__6567;
chunk__6530_6542 = G__6568;
count__6531_6543 = G__6569;
i__6532_6544 = G__6570;
continue;
} else {
var temp__4126__auto___6571 = cljs.core.seq.call(null,seq__6529_6541);
if(temp__4126__auto___6571){
var seq__6529_6572__$1 = temp__4126__auto___6571;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__6529_6572__$1)){
var c__4175__auto___6573 = cljs.core.chunk_first.call(null,seq__6529_6572__$1);
var G__6574 = cljs.core.chunk_rest.call(null,seq__6529_6572__$1);
var G__6575 = c__4175__auto___6573;
var G__6576 = cljs.core.count.call(null,c__4175__auto___6573);
var G__6577 = (0);
seq__6529_6541 = G__6574;
chunk__6530_6542 = G__6575;
count__6531_6543 = G__6576;
i__6532_6544 = G__6577;
continue;
} else {
var source_6578 = cljs.core.first.call(null,seq__6529_6572__$1);
if(cljs.core.truth_(tailrecursion.javelin.cell_QMARK_.call(null,source_6578))){
source_6578.sinks = cljs.core.conj.call(null,source_6578.sinks,this$);

if((source_6578.rank > this$.rank)){
var seq__6537_6579 = cljs.core.seq.call(null,tailrecursion.javelin.bf_seq.call(null,cljs.core.identity,((function (seq__6529_6541,chunk__6530_6542,count__6531_6543,i__6532_6544,source_6578,seq__6529_6572__$1,temp__4126__auto___6571,vec__6528,f,sources){
return (function (p1__6512_SHARP_){
return p1__6512_SHARP_.sinks;
});})(seq__6529_6541,chunk__6530_6542,count__6531_6543,i__6532_6544,source_6578,seq__6529_6572__$1,temp__4126__auto___6571,vec__6528,f,sources))
,source_6578));
var chunk__6538_6580 = null;
var count__6539_6581 = (0);
var i__6540_6582 = (0);
while(true){
if((i__6540_6582 < count__6539_6581)){
var dep_6583 = cljs.core._nth.call(null,chunk__6538_6580,i__6540_6582);
dep_6583.rank = tailrecursion.javelin.next_rank.call(null);

var G__6584 = seq__6537_6579;
var G__6585 = chunk__6538_6580;
var G__6586 = count__6539_6581;
var G__6587 = (i__6540_6582 + (1));
seq__6537_6579 = G__6584;
chunk__6538_6580 = G__6585;
count__6539_6581 = G__6586;
i__6540_6582 = G__6587;
continue;
} else {
var temp__4126__auto___6588__$1 = cljs.core.seq.call(null,seq__6537_6579);
if(temp__4126__auto___6588__$1){
var seq__6537_6589__$1 = temp__4126__auto___6588__$1;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__6537_6589__$1)){
var c__4175__auto___6590 = cljs.core.chunk_first.call(null,seq__6537_6589__$1);
var G__6591 = cljs.core.chunk_rest.call(null,seq__6537_6589__$1);
var G__6592 = c__4175__auto___6590;
var G__6593 = cljs.core.count.call(null,c__4175__auto___6590);
var G__6594 = (0);
seq__6537_6579 = G__6591;
chunk__6538_6580 = G__6592;
count__6539_6581 = G__6593;
i__6540_6582 = G__6594;
continue;
} else {
var dep_6595 = cljs.core.first.call(null,seq__6537_6589__$1);
dep_6595.rank = tailrecursion.javelin.next_rank.call(null);

var G__6596 = cljs.core.next.call(null,seq__6537_6589__$1);
var G__6597 = null;
var G__6598 = (0);
var G__6599 = (0);
seq__6537_6579 = G__6596;
chunk__6538_6580 = G__6597;
count__6539_6581 = G__6598;
i__6540_6582 = G__6599;
continue;
}
} else {
}
}
break;
}
} else {
}
} else {
}

var G__6600 = cljs.core.next.call(null,seq__6529_6572__$1);
var G__6601 = null;
var G__6602 = (0);
var G__6603 = (0);
seq__6529_6541 = G__6600;
chunk__6530_6542 = G__6601;
count__6531_6543 = G__6602;
i__6532_6544 = G__6603;
continue;
}
} else {
}
}
break;
}

var compute_6604 = ((function (vec__6528,f,sources){
return (function (p1__6513_SHARP_){
return cljs.core.apply.call(null,tailrecursion.javelin.deref_STAR_.call(null,cljs.core.peek.call(null,p1__6513_SHARP_)),cljs.core.map.call(null,tailrecursion.javelin.deref_STAR_,cljs.core.pop.call(null,p1__6513_SHARP_)));
});})(vec__6528,f,sources))
;
this$.thunk = ((function (compute_6604,vec__6528,f,sources){
return (function (){
return this$.state = compute_6604.call(null,this$.sources);
});})(compute_6604,vec__6528,f,sources))
;
} else {
}

return tailrecursion.javelin.propagate_BANG_.call(null,this$);
};
var set_formula_BANG_ = function (this$,var_args){
var p__6514 = null;
if (arguments.length > 1) {
  p__6514 = cljs.core.array_seq(Array.prototype.slice.call(arguments, 1),0);
} 
return set_formula_BANG___delegate.call(this,this$,p__6514);};
set_formula_BANG_.cljs$lang$maxFixedArity = 1;
set_formula_BANG_.cljs$lang$applyTo = (function (arglist__6605){
var this$ = cljs.core.first(arglist__6605);
var p__6514 = cljs.core.rest(arglist__6605);
return set_formula_BANG___delegate(this$,p__6514);
});
set_formula_BANG_.cljs$core$IFn$_invoke$arity$variadic = set_formula_BANG___delegate;
return set_formula_BANG_;
})()
;

/**
* @constructor
*/
tailrecursion.javelin.Cell = (function (meta,state,rank,prev,sources,sinks,thunk,watches,update){
this.meta = meta;
this.state = state;
this.rank = rank;
this.prev = prev;
this.sources = sources;
this.sinks = sinks;
this.thunk = thunk;
this.watches = watches;
this.update = update;
this.cljs$lang$protocol_mask$partition1$ = 98306;
this.cljs$lang$protocol_mask$partition0$ = 2147647488;
})
tailrecursion.javelin.Cell.prototype.cljs$core$IWatchable$_notify_watches$arity$3 = (function (this$,o,n){
var self__ = this;
var this$__$1 = this;
var seq__6606 = cljs.core.seq.call(null,self__.watches);
var chunk__6607 = null;
var count__6608 = (0);
var i__6609 = (0);
while(true){
if((i__6609 < count__6608)){
var vec__6610 = cljs.core._nth.call(null,chunk__6607,i__6609);
var key = cljs.core.nth.call(null,vec__6610,(0),null);
var f = cljs.core.nth.call(null,vec__6610,(1),null);
f.call(null,key,this$__$1,o,n);

var G__6612 = seq__6606;
var G__6613 = chunk__6607;
var G__6614 = count__6608;
var G__6615 = (i__6609 + (1));
seq__6606 = G__6612;
chunk__6607 = G__6613;
count__6608 = G__6614;
i__6609 = G__6615;
continue;
} else {
var temp__4126__auto__ = cljs.core.seq.call(null,seq__6606);
if(temp__4126__auto__){
var seq__6606__$1 = temp__4126__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__6606__$1)){
var c__4175__auto__ = cljs.core.chunk_first.call(null,seq__6606__$1);
var G__6616 = cljs.core.chunk_rest.call(null,seq__6606__$1);
var G__6617 = c__4175__auto__;
var G__6618 = cljs.core.count.call(null,c__4175__auto__);
var G__6619 = (0);
seq__6606 = G__6616;
chunk__6607 = G__6617;
count__6608 = G__6618;
i__6609 = G__6619;
continue;
} else {
var vec__6611 = cljs.core.first.call(null,seq__6606__$1);
var key = cljs.core.nth.call(null,vec__6611,(0),null);
var f = cljs.core.nth.call(null,vec__6611,(1),null);
f.call(null,key,this$__$1,o,n);

var G__6620 = cljs.core.next.call(null,seq__6606__$1);
var G__6621 = null;
var G__6622 = (0);
var G__6623 = (0);
seq__6606 = G__6620;
chunk__6607 = G__6621;
count__6608 = G__6622;
i__6609 = G__6623;
continue;
}
} else {
return null;
}
}
break;
}
});

tailrecursion.javelin.Cell.prototype.cljs$core$IWatchable$_add_watch$arity$3 = (function (this$,k,f){
var self__ = this;
var this$__$1 = this;
return this$__$1.watches = cljs.core.assoc.call(null,self__.watches,k,f);
});

tailrecursion.javelin.Cell.prototype.cljs$core$IWatchable$_remove_watch$arity$2 = (function (this$,k){
var self__ = this;
var this$__$1 = this;
return this$__$1.watches = cljs.core.dissoc.call(null,self__.watches,k);
});

tailrecursion.javelin.Cell.prototype.cljs$core$ISwap$_swap_BANG_$arity$2 = (function (this$,f){
var self__ = this;
var this$__$1 = this;
return cljs.core.reset_BANG_.call(null,this$__$1,f.call(null,this$__$1.state));
});

tailrecursion.javelin.Cell.prototype.cljs$core$ISwap$_swap_BANG_$arity$3 = (function (this$,f,a){
var self__ = this;
var this$__$1 = this;
return cljs.core.reset_BANG_.call(null,this$__$1,f.call(null,this$__$1.state,a));
});

tailrecursion.javelin.Cell.prototype.cljs$core$ISwap$_swap_BANG_$arity$4 = (function (this$,f,a,b){
var self__ = this;
var this$__$1 = this;
return cljs.core.reset_BANG_.call(null,this$__$1,f.call(null,this$__$1.state,a,b));
});

tailrecursion.javelin.Cell.prototype.cljs$core$ISwap$_swap_BANG_$arity$5 = (function (this$,f,a,b,xs){
var self__ = this;
var this$__$1 = this;
return cljs.core.reset_BANG_.call(null,this$__$1,cljs.core.apply.call(null,f,this$__$1.state,a,b,xs));
});

tailrecursion.javelin.Cell.prototype.cljs$core$IReset$_reset_BANG_$arity$2 = (function (this$,x){
var self__ = this;
var this$__$1 = this;
if(cljs.core.truth_(tailrecursion.javelin.lens_QMARK_.call(null,this$__$1))){
this$__$1.update.call(null,x);
} else {
if(cljs.core.truth_(tailrecursion.javelin.input_QMARK_.call(null,this$__$1))){
this$__$1.state = x;

tailrecursion.javelin.propagate_BANG_.call(null,this$__$1);
} else {
throw (new Error("can't swap! or reset! formula cell"));

}
}

return this$__$1.state;
});

tailrecursion.javelin.Cell.prototype.cljs$core$IDeref$_deref$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return this$__$1.state;
});

tailrecursion.javelin.Cell.prototype.cljs$core$IMeta$_meta$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.meta;
});

tailrecursion.javelin.Cell.prototype.cljs$core$IPrintWithWriter$_pr_writer$arity$3 = (function (this$,w,_){
var self__ = this;
var this$__$1 = this;
return cljs.core.write_all.call(null,w,"#<Cell: ",cljs.core.pr_str.call(null,self__.state),">");
});

tailrecursion.javelin.Cell.cljs$lang$type = true;

tailrecursion.javelin.Cell.cljs$lang$ctorStr = "tailrecursion.javelin/Cell";

tailrecursion.javelin.Cell.cljs$lang$ctorPrWriter = (function (this__3975__auto__,writer__3976__auto__,opt__3977__auto__){
return cljs.core._write.call(null,writer__3976__auto__,"tailrecursion.javelin/Cell");
});

tailrecursion.javelin.__GT_Cell = (function __GT_Cell(meta,state,rank,prev,sources,sinks,thunk,watches,update){
return (new tailrecursion.javelin.Cell(meta,state,rank,prev,sources,sinks,thunk,watches,update));
});

tailrecursion.javelin.cell_QMARK_ = (function cell_QMARK_(c){
if(cljs.core._EQ_.call(null,cljs.core.type.call(null,c),tailrecursion.javelin.Cell)){
return c;
} else {
return null;
}
});
tailrecursion.javelin.formula_QMARK_ = (function formula_QMARK_(c){
if(cljs.core.truth_((function (){var and__3381__auto__ = tailrecursion.javelin.cell_QMARK_.call(null,c);
if(cljs.core.truth_(and__3381__auto__)){
return c.thunk;
} else {
return and__3381__auto__;
}
})())){
return c;
} else {
return null;
}
});
tailrecursion.javelin.lens_QMARK_ = (function lens_QMARK_(c){
if(cljs.core.truth_((function (){var and__3381__auto__ = tailrecursion.javelin.cell_QMARK_.call(null,c);
if(cljs.core.truth_(and__3381__auto__)){
return c.update;
} else {
return and__3381__auto__;
}
})())){
return c;
} else {
return null;
}
});
tailrecursion.javelin.input_QMARK_ = (function input_QMARK_(c){
if(cljs.core.truth_((function (){var and__3381__auto__ = tailrecursion.javelin.cell_QMARK_.call(null,c);
if(cljs.core.truth_(and__3381__auto__)){
return cljs.core.not.call(null,tailrecursion.javelin.formula_QMARK_.call(null,c));
} else {
return and__3381__auto__;
}
})())){
return c;
} else {
return null;
}
});
tailrecursion.javelin.set_cell_BANG_ = (function set_cell_BANG_(c,x){
c.state = x;

return tailrecursion.javelin.set_formula_BANG_.call(null,c);
});
tailrecursion.javelin.formula = (function formula(f){
return (function() { 
var G__6624__delegate = function (sources){
return tailrecursion.javelin.set_formula_BANG_.call(null,tailrecursion.javelin.cell.call(null,new cljs.core.Keyword("tailrecursion.javelin","none","tailrecursion.javelin/none",273761139)),f,sources);
};
var G__6624 = function (var_args){
var sources = null;
if (arguments.length > 0) {
  sources = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0),0);
} 
return G__6624__delegate.call(this,sources);};
G__6624.cljs$lang$maxFixedArity = 0;
G__6624.cljs$lang$applyTo = (function (arglist__6625){
var sources = cljs.core.seq(arglist__6625);
return G__6624__delegate(sources);
});
G__6624.cljs$core$IFn$_invoke$arity$variadic = G__6624__delegate;
return G__6624;
})()
;
});
tailrecursion.javelin.lens = (function lens(c,f){
var c__$1 = tailrecursion.javelin.formula.call(null,cljs.core.identity).call(null,c);
c__$1.update = f;

return c__$1;
});
tailrecursion.javelin.cell = (function cell(x){
return tailrecursion.javelin.set_formula_BANG_.call(null,(new tailrecursion.javelin.Cell(cljs.core.PersistentArrayMap.EMPTY,x,tailrecursion.javelin.next_rank.call(null),x,cljs.core.PersistentVector.EMPTY,cljs.core.PersistentHashSet.EMPTY,null,cljs.core.PersistentArrayMap.EMPTY,null)));
});
tailrecursion.javelin.lift = tailrecursion.javelin.formula;
tailrecursion.javelin.dosync_STAR_ = (function dosync_STAR_(thunk){
var bind = (function (p1__6626_SHARP_){
var _STAR_tx_STAR_6629 = tailrecursion.javelin._STAR_tx_STAR_;
try{tailrecursion.javelin._STAR_tx_STAR_ = cljs.core.atom.call(null,tailrecursion.priority_map.priority_map.call(null));

return p1__6626_SHARP_.call(null);
}finally {tailrecursion.javelin._STAR_tx_STAR_ = _STAR_tx_STAR_6629;
}});
var prop = ((function (bind){
return (function (){
var tx = cljs.core.deref.call(null,tailrecursion.javelin._STAR_tx_STAR_);
var _STAR_tx_STAR_6630 = tailrecursion.javelin._STAR_tx_STAR_;
try{tailrecursion.javelin._STAR_tx_STAR_ = null;

return tailrecursion.javelin.propagate_STAR_.call(null,tx);
}finally {tailrecursion.javelin._STAR_tx_STAR_ = _STAR_tx_STAR_6630;
}});})(bind))
;
if(cljs.core.truth_(tailrecursion.javelin._STAR_tx_STAR_)){
return thunk.call(null);
} else {
return bind.call(null,((function (bind,prop){
return (function (){
thunk.call(null);

return prop.call(null);
});})(bind,prop))
);
}
});
/**
* @param {...*} var_args
*/
tailrecursion.javelin.alts_BANG_ = (function() { 
var alts_BANG___delegate = function (cells){
var olds = cljs.core.atom.call(null,cljs.core.repeat.call(null,cljs.core.count.call(null,cells),new cljs.core.Keyword("tailrecursion.javelin","none","tailrecursion.javelin/none",273761139)));
var tag_neq = ((function (olds){
return (function (p1__6631_SHARP_,p2__6632_SHARP_){
return (new cljs.core.PersistentVector(null,2,(5),cljs.core.PersistentVector.EMPTY_NODE,[cljs.core.not_EQ_.call(null,p1__6631_SHARP_,p2__6632_SHARP_),p2__6632_SHARP_],null));
});})(olds))
;
var diff = ((function (olds,tag_neq){
return (function (p1__6634_SHARP_,p2__6633_SHARP_){
return cljs.core.distinct.call(null,cljs.core.map.call(null,cljs.core.second,cljs.core.filter.call(null,cljs.core.first,cljs.core.map.call(null,tag_neq,p1__6634_SHARP_,p2__6633_SHARP_))));
});})(olds,tag_neq))
;
var proc = ((function (olds,tag_neq,diff){
return (function() { 
var G__6636__delegate = function (rest__6635_SHARP_){
var news = diff.call(null,cljs.core.deref.call(null,olds),rest__6635_SHARP_);
cljs.core.reset_BANG_.call(null,olds,rest__6635_SHARP_);

return news;
};
var G__6636 = function (var_args){
var rest__6635_SHARP_ = null;
if (arguments.length > 0) {
  rest__6635_SHARP_ = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0),0);
} 
return G__6636__delegate.call(this,rest__6635_SHARP_);};
G__6636.cljs$lang$maxFixedArity = 0;
G__6636.cljs$lang$applyTo = (function (arglist__6637){
var rest__6635_SHARP_ = cljs.core.seq(arglist__6637);
return G__6636__delegate(rest__6635_SHARP_);
});
G__6636.cljs$core$IFn$_invoke$arity$variadic = G__6636__delegate;
return G__6636;
})()
;})(olds,tag_neq,diff))
;
return cljs.core.apply.call(null,tailrecursion.javelin.formula.call(null,proc),cells);
};
var alts_BANG_ = function (var_args){
var cells = null;
if (arguments.length > 0) {
  cells = cljs.core.array_seq(Array.prototype.slice.call(arguments, 0),0);
} 
return alts_BANG___delegate.call(this,cells);};
alts_BANG_.cljs$lang$maxFixedArity = 0;
alts_BANG_.cljs$lang$applyTo = (function (arglist__6638){
var cells = cljs.core.seq(arglist__6638);
return alts_BANG___delegate(cells);
});
alts_BANG_.cljs$core$IFn$_invoke$arity$variadic = alts_BANG___delegate;
return alts_BANG_;
})()
;
tailrecursion.javelin.cell_map = (function cell_map(f,c){
var cseq = tailrecursion.javelin.formula.call(null,cljs.core.seq).call(null,c);
return cljs.core.map.call(null,((function (cseq){
return (function (p1__6639_SHARP_){
return tailrecursion.javelin.formula.call(null,cljs.core.comp.call(null,f,tailrecursion.javelin.safe_nth)).call(null,cseq,p1__6639_SHARP_);
});})(cseq))
,cljs.core.range.call(null,(0),cljs.core.count.call(null,cljs.core.deref.call(null,cseq))));
});
tailrecursion.javelin.cell_doseq_STAR_ = (function cell_doseq_STAR_(items_seq,f){
var pool_size = tailrecursion.javelin.cell.call(null,(0));
var cur_count = tailrecursion.javelin.formula.call(null,cljs.core.count).call(null,items_seq);
var ith_item = ((function (pool_size,cur_count){
return (function (p1__6640_SHARP_){
return tailrecursion.javelin.formula.call(null,tailrecursion.javelin.safe_nth).call(null,items_seq,p1__6640_SHARP_);
});})(pool_size,cur_count))
;
return tailrecursion.javelin.formula.call(null,((function (pool_size,cur_count,ith_item){
return (function (pool_size__$1,cur_count__$1,f__$1,ith_item__$1,reset_pool_size_BANG_){
if((pool_size__$1 < cur_count__$1)){
var seq__6645_6649 = cljs.core.seq.call(null,cljs.core.range.call(null,pool_size__$1,cur_count__$1));
var chunk__6646_6650 = null;
var count__6647_6651 = (0);
var i__6648_6652 = (0);
while(true){
if((i__6648_6652 < count__6647_6651)){
var i_6653 = cljs.core._nth.call(null,chunk__6646_6650,i__6648_6652);
f__$1.call(null,ith_item__$1.call(null,i_6653));

var G__6654 = seq__6645_6649;
var G__6655 = chunk__6646_6650;
var G__6656 = count__6647_6651;
var G__6657 = (i__6648_6652 + (1));
seq__6645_6649 = G__6654;
chunk__6646_6650 = G__6655;
count__6647_6651 = G__6656;
i__6648_6652 = G__6657;
continue;
} else {
var temp__4126__auto___6658 = cljs.core.seq.call(null,seq__6645_6649);
if(temp__4126__auto___6658){
var seq__6645_6659__$1 = temp__4126__auto___6658;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__6645_6659__$1)){
var c__4175__auto___6660 = cljs.core.chunk_first.call(null,seq__6645_6659__$1);
var G__6661 = cljs.core.chunk_rest.call(null,seq__6645_6659__$1);
var G__6662 = c__4175__auto___6660;
var G__6663 = cljs.core.count.call(null,c__4175__auto___6660);
var G__6664 = (0);
seq__6645_6649 = G__6661;
chunk__6646_6650 = G__6662;
count__6647_6651 = G__6663;
i__6648_6652 = G__6664;
continue;
} else {
var i_6665 = cljs.core.first.call(null,seq__6645_6659__$1);
f__$1.call(null,ith_item__$1.call(null,i_6665));

var G__6666 = cljs.core.next.call(null,seq__6645_6659__$1);
var G__6667 = null;
var G__6668 = (0);
var G__6669 = (0);
seq__6645_6649 = G__6666;
chunk__6646_6650 = G__6667;
count__6647_6651 = G__6668;
i__6648_6652 = G__6669;
continue;
}
} else {
}
}
break;
}

return reset_pool_size_BANG_.call(null,cur_count__$1);
} else {
return null;
}
});})(pool_size,cur_count,ith_item))
).call(null,pool_size,cur_count,f,ith_item,cljs.core.partial.call(null,cljs.core.reset_BANG_,pool_size));
});
