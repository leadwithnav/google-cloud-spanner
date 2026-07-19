const functions = require('@google-cloud/functions-framework');

functions.http('formatCurrency', (req, res) => {
  const requestBody = req.body;
  const calls = requestBody.calls || [];
  
  // Process batch rows: each argument set is wrapped in an array
  const replies = calls.map(args => {
    const amount = parseFloat(args[0]);
    if (isNaN(amount)) return null;
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
  });
  
  res.status(200).json({ replies });
});