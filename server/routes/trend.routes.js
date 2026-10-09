const router = require('express').Router();

router.get('/', (req, res) => res.json({ ok: 'trend' }));

module.exports = router;
