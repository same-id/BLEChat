var express = require('express');
var logger = require('morgan');
var multer = require('multer');

var app = express();

app.use(logger('dev'));
app.use(multer());

var messages = {};

app.put('/message/:msg_id', function (req, res) {
  var msg_id = req.params.msg_id;
  var msg = req.body.msg;

  console.log("here");
  console.log(req.body);

  if (messages[msg_id]) {
    res.sendStatus(500);
    return;
  }

  messages[msg_id] = msg;

  console.log(messages);

  res.sendStatus(200);
});

app.get('/message/:msg_id', function (req, res) {

  msg_id = req.params.msg_id;

  if (!messages[msg_id]) {
    res.sendStatus(404);
    return;
  }

  res.status(200);
  res.send(messages[msg_id]);

});

// TODO: Add a scheduled task that cleans up old message entries

// catch 404 and forward to error handler
app.use(function(req, res, next) {
  return res.status(404).json({ error : 'Page not found' });
});

// error handlers

// development error handler
// will print stacktrace
if (app.get('env') === 'development') {
  app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
      message: err.message,
      error: err
    });
  });
}

// production error handler
// no stacktraces leaked to user
app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.render('error', {
	message: err.message,
	error: {}
  });
});


module.exports = app;
