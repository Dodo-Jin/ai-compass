const router = require('express').Router();

router.get('/', (req, res) => res.json({ ok: 'auth' }));

module.exports = router;
