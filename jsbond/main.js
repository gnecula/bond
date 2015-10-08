var STRIP_COMMENTS = /(\/\/.*$)|(\/\*[\s\S]*?\*\/)|(\s*=[^,\)]*(('(?:\\'|[^'\r\n])*')|("(?:\\"|[^"\r\n])*"))|(\s*=[^,\)]*))/mg;
var ARGUMENT_NAMES = /([^\s,]+)/g;
function getParamNames(func) {
    var fnStr = func.toString().replace(STRIP_COMMENTS, '');
    var result = fnStr.slice(fnStr.indexOf('(')+1, fnStr.indexOf(')')).match(ARGUMENT_NAMES);
    if(result === null)
        result = [];
    return result;
}

function observe(spyPointName) {
    console.log(spyPointName + ': ' + Array.prototype.slice.call(arguments).slice(1).join(', '))
}

function observeFunction(func, spyPointName) {
    var self = this;
    var paramNames = getParamNames(func);
    var pointName = spyPointName === undefined ? func.name : spyPointName;
    return function() {
        var observeStrings = [];
        for (var i = 0; i < arguments.length; i++) {
            var paramString;
            if (i >= paramNames.length) {
                paramString = '';
            } else {
                paramString = paramNames[i] + '=';
            }
            observeStrings.push(paramString + arguments[i]);
        }
        observe(pointName, observeStrings);
        func.apply(self, arguments)
    }
}

function myFunc(arg1, arg2) {
    console.log('inside myFunc with arg1=' + arg1 + ', arg2=' + arg2)
}
myFunc = observeFunction(myFunc);

myFunc('blaj', 'hello', 54, 5);
