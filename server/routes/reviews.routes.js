const router = require('express').Router();

router.get('/', (req, res) => res.json({ ok: 'reviews' }));

module.exports = router;
