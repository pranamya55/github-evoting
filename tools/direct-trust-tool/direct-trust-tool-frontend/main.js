/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
const {app, BrowserWindow, Menu} = require('electron');
const url = require('url');
const path = require('path');

let child = null;
function startBackend() {
  child = require('child_process').spawn('resources\\embedded-jre\\bin\\java.exe', ['-jar', 'resources\\direct-trust-tool-backend-runnable.jar', '']);
  child.stderr.on('data', function (data) {
    console.log('error', data.toString());
  });
  child.stdout.on('data', function (data) {
    console.log('info', data.toString());
  });
}

let mainWindow = null;
const prepareWindow = function () {
  mainWindow = new BrowserWindow({
    show: false,
    width: 1350,
    height: 800,
    webPreferences: {
      plugins: true
    },
  });
  mainWindow.maximize();
  mainWindow.webContents.on('did-finish-load', () => mainWindow.setTitle('Direct Trust Tool'));
  Menu.setApplicationMenu(Menu.buildFromTemplate([
    {
      label: 'File',
      submenu: [
        {
          label: 'Toggle developer tools', click() {
            mainWindow.webContents.toggleDevTools();
          },
          accelerator: 'F12'
        },
        {
          label: 'Exit', click() {
            app.quit();
          }
        }
      ]
    }
  ]));

  mainWindow.loadURL(url.format({
    pathname: path.join(__dirname, 'index.html'), protocol: 'file:', slashes: true
  }))
};

app.on('ready', function () {
  startBackend();
  prepareWindow();
});

app.on('before-quit', function() {
  child.kill();
});
