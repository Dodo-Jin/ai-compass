const router = require('express').Router();

router.get('/', (req, res) => res.json({ ok: 'library' }));

module.exports = router;
