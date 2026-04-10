const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlInlineScriptPlugin = require('html-inline-script-webpack-plugin');

module.exports = {
  mode: 'development',
  devtool: false,
  stats: 'errors-only',

  entry: {
    simple_hub: './src/simple_hub.ts'
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
      template: './src/simple_hub.html',
      filename: 'simple_hub.html',
      chunks: ['simple_hub'],
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
    open: ['/hub'],
    client: {
      logging: 'none',
      overlay: false,
      progress: false,
    },
    historyApiFallback: {
      rewrites: [
        // ИЗМЕНЕНО: обновлены правила перезаписи путей для dev-сервера
        { from: /^\/simple/, to: '/simple_hub.html' },
        { from: /^\/complex/, to: '/simple_hub.html' },
        { from: /^\/hub/, to: '/simple_hub.html' }
      ]
    }
  }
};