const http = require('http');

const PORT = process.env.PORT || 8888;
const HOST = process.env.HOST || '0.0.0.0';
const TIMEOUT = process.env.TIMEOUT;

http.createServer((req, res) => {
  const { pathname, searchParams } = new URL(req.url, `http://${req.headers.host}`);
  const method = req.method;
  const headers = JSON.stringify(req.headers);

  let queryParams = []
  for (const name of searchParams.keys())
    queryParams.push({ "name": name, "value": searchParams.getAll(name)});

  requestParams = JSON.stringify(queryParams);

  const msg = `m=request method=${method} path=${pathname}, queryParams=${requestParams} headers=${headers}, serverPort=${PORT}`;
  console.log(msg);

  const sendResponse = () => {
    if (pathname == '/modify') {
      res.writeHead(401).end();
    } else {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        "method": method,
        "path": pathname,
        "queryParams": queryParams,
        "headers": req.headers,
        "server": {
            "host": HOST,
            "port": PORT,
            "timeout": TIMEOUT
        }
      }));
    }

  }

  if (TIMEOUT) setTimeout(sendResponse, TIMEOUT);
  else sendResponse();
})
.listen(PORT, HOST, () => console.log(`Server listen on http://${HOST}:${PORT}`));
