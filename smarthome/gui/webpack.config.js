const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
// ИСПРАВЛЕНО: Правильное имя пакета
const HtmlInlineScriptPlugin = require('html-inline-script-webpack-plugin');

module.exports = {
  mode: 'production',

  entry: './src/index.ts',

  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
    ],
  },

  resolve: {
    extensions: ['.tsx', '.ts', '.js'],
    fallback: {
      "crypto": false
    }
  },

  output: {
    filename: 'bundle.js',
    path: path.resolve(__dirname, 'dist'),
    clean: true,
  },

  plugins: [
    new HtmlWebpackPlugin({
      template: './src/index.html',
      filename: 'index.html',
      inject: 'body',
      minify: false
    }),
    // Встраивает JS (bundle.js) прямо внутрь HTML
    new HtmlInlineScriptPlugin()
  ],

  devServer: {
    static: {
      directory: path.join(__dirname, './dist'),
    },
    open: true,
  }
};