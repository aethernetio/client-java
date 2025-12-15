const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlInlineScriptPlugin = require('html-inline-script-webpack-plugin');

module.exports = {
  mode: 'development',
  devtool: false,
  stats: 'errors-only',

  entry: {
    simple: './src/simple.ts',
    complex: './src/complex.ts',
  },

  optimization: {
    minimize: false,
    moduleIds: 'named',
    chunkIds: 'named',
  },

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
    filename: '[name].bundle.js',
    path: path.resolve(__dirname, 'dist'),
    clean: true,
    publicPath: '',
    pathinfo: false,
  },

  plugins: [
    new HtmlWebpackPlugin({
      template: './src/simple.html',
      // ИЗМЕНЕНО: simple.html -> temp_test_plain.html
      filename: 'temp_test_plain.html',
      chunks: ['simple'],
      inject: 'body',
      minify: false
    }),
    new HtmlWebpackPlugin({
      template: './src/complex.html',
      // ИЗМЕНЕНО: complex.html -> temp_test.html
      filename: 'temp_test.html',
      chunks: ['complex'],
      inject: 'body',
      minify: false
    }),
    new HtmlInlineScriptPlugin()
  ],

  devServer: {
    static: {
      directory: path.join(__dirname, './dist'),
    },
    // ИЗМЕНЕНО: открываем новый файл по умолчанию
    open: ['/temp_test_plain.html'],
    client: {
      logging: 'none',
      overlay: false,
      progress: false,
    },
    historyApiFallback: {
      rewrites: [
        // ИЗМЕНЕНО: обновлены правила перезаписи путей для dev-сервера
        { from: /^\/simple/, to: '/temp_test_plain.html' },
        { from: /^\/complex/, to: '/temp_test.html' },
      ]
    }
  }
};