"use strict";

var execBtn = document.getElementById("execute");
var downloadDbBtn = document.getElementById("download-db");
var outputElm = document.getElementById('output');
var errorElm = document.getElementById('error');
var commandsElm = document.getElementById('commands');


// Open a database
var config = {
  locateFile: (filename, prefix) => {
    console.log(`prefix is : ${prefix}`);
    return 'https://cdnjs.cloudflare.com/ajax/libs/sql.js/1.8.0/sql-wasm.wasm';
  }
};

var db = null;

// Download database
function DownloadDatabase() {
	// The `initSqlJs` function is globally provided by all of the main dist files if loaded in the browser.
	// We must specify this locateFile function if we are loading a wasm file from anywhere other than the current html page's folder.
	const sqlPromise = initSqlJs(config);

	// Open a database
	const databaseUrlPrefix = "https://hixon10.github.io/openjdk-mailing-lists-search/";
	const databasePartNames = ["db-part-00", "db-part-01", "db-part-02", "db-part-03", "db-part-04", "db-part-05", "db-part-06", "db-part-07", "db-part-08", "db-part-09"];

	const databasePartPromises = [];

	for (const dbPartName of databasePartNames) {
		// disable cache https://stackoverflow.com/a/59493583/1756750
		const ms = Date.now();
		const currentPartUrl = databaseUrlPrefix+dbPartName+".zip?dummy="+ms;
		const currentDbPartPromise = fetch(currentPartUrl, {
			  headers: {
				'Cache-Control': 'no-cache',
				'pragma': 'no-cache'
			  }
			})
			.then(res => res.blob())
			.then(JSZip.loadAsync)
			.then(function (zip) {
                return zip.file(dbPartName).async("uint8array");
            })
			.catch(function(err) {
                console.error("Some error:", err);
            });
		databasePartPromises.push(currentDbPartPromise);
	}

	console.log("Start downloading a database...");
	downloadDbBtn.disabled = true;
	downloadDbBtn.classList.remove("button");

	const dataPromise = Promise.all(databasePartPromises);

	Promise.all([sqlPromise, dataPromise]).then((values) => {
		const SQL = values[0];
		const databaseParts = values[1];
		
		// https://stackoverflow.com/a/49129872/1756750
		  let length = 0;
		  databaseParts.forEach(item => {
			  length += item.length;
		  });
		  
		  let mergedArray = new Uint8Array(length);
		  let offset = 0;
		  databaseParts.forEach(item => {
			  mergedArray.set(item, offset);
			  offset += item.length;
		  });

		db = new SQL.Database(mergedArray);
		console.log("Database is ready");
		
		
		execBtn.disabled = false; 
		execBtn.classList.add("button");
	});

}
downloadDbBtn.addEventListener("click", DownloadDatabase, true);



// Connect to the HTML element we 'print' to
function print(text) {
	outputElm.innerHTML = text.replace(/\n/g, '<br>');
}
function error(e) {
	console.log(e);
	errorElm.style.height = '2em';
	errorElm.textContent = e.message;
}

function noerror() {
	errorElm.style.height = '0';
}

// Run a command in the database
function execute(commands) {
	tic();
	
	toc("Executing SQL");
	
	outputElm.textContent = "Fetching results...";
	
	
	try {
		var stmt = db.prepare(commands);

		var columns = null;
		const rows = [];

		while (stmt.step()) { 
		  if (columns == null) {
			  columns = stmt.getColumnNames();
		  }
		  rows.push(stmt.get(null, {useBigInt: true}));
		}
		
		tic();
		
		if (columns != null) {
			outputElm.innerHTML = "";
			outputElm.appendChild(tableCreate(columns, rows));
			toc("Displaying results");
		}
	} catch (e) {
	  error(e);
	}
}

// Create an HTML table
var tableCreate = function () {
	function valconcat(vals, tagName) {
		if (vals.length === 0) return '';
		var open = '<' + tagName + '>', close = '</' + tagName + '>';
		return open + vals.join(close + open) + close;
	}
	return function (columns, values) {
		var tbl = document.createElement('table');
		var html = '<thead>' + valconcat(columns, 'th') + '</thead>';
		var rows = values.map(function (v) { return valconcat(v, 'td'); });
		html += '<tbody>' + valconcat(rows, 'tr') + '</tbody>';
		tbl.innerHTML = html;
		return tbl;
	}
}();

// Execute the commands when the button is clicked
function execEditorContents() {
	noerror();
	execute(editor.getValue() + ';');
}
execBtn.addEventListener("click", execEditorContents, true);

// Performance measurement functions
var tictime;
if (!window.performance || !performance.now) { window.performance = { now: Date.now } }
function tic() { tictime = performance.now() }
function toc(msg) {
	var dt = performance.now() - tictime;
	console.log((msg || 'toc') + ": " + dt + "ms");
}

// Add syntax highlihjting to the textarea
var editor = CodeMirror.fromTextArea(commandsElm, {
	mode: 'text/x-mysql',
	viewportMargin: Infinity,
	indentWithTabs: true,
	smartIndent: true,
	lineNumbers: true,
	matchBrackets: true,
	autofocus: true,
	extraKeys: {
		"Ctrl-Enter": execEditorContents,
	}
});


