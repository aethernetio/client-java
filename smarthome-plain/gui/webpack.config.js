const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlInlineScriptPlugin = require('html-inline-script-webpack-plugin');

module.exports = {
  mode: 'production',

  entry: './src/index.ts',

  stats: 'errors-only',

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
    publicPath: '',
  },

  plugins: [
    new HtmlWebpackPlugin({
      template: './src/index.html',
      filename: 'index.html',
      inject: 'body',
      minify: false
    }),
    new HtmlInlineScriptPlugin()
  ],

  devServer: {
    static: {
      directory: path.join(__dirname, './dist'),
    },
    open: true,
    client: {
      logging: 'none',
      overlay: false,
      progress: false,
    },
  }
};