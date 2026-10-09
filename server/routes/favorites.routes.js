const router = require('express').Router();

router.get('/', (req, res) => res.json({ ok: 'favorites' }));

module.exports = router;
