require('dotenv').config({ path: require('path').join(__dirname, '../.env') });
const fs = require('fs');
const path = require('path');
const express = require('express');

const app = express();
app.use(express.json());
app.use(express.static(path.join(__dirname, '../client')));

// routes/<name>.routes.js -> /api/<name> 자동 등록 (이 파일은 수정할 일이 거의 없음)
const dir = path.join(__dirname, 'routes');
for (const f of fs.readdirSync(dir).filter(f => f.endsWith('.routes.js'))) {
  app.use('/api/' + f.replace('.routes.js', ''), require(path.join(dir, f)));
}

app.use((err, req, res, next) => res.status(err.status || 500).json({ message: err.message }));
app.listen(process.env.PORT || 3000, () => console.log('http://localhost:' + (process.env.PORT || 3000)));
