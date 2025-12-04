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
      filename: 'simple.html',
      chunks: ['simple'],
      inject: 'body',
      minify: false
    }),
    new HtmlWebpackPlugin({
      template: './src/complex.html',
      filename: 'complex.html',
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
    open: ['/simple.html'],
    client: {
      logging: 'none',
      overlay: false,
      progress: false,
    },
    historyApiFallback: {
      rewrites: [
        { from: /^\/simple/, to: '/simple.html' },
        { from: /^\/complex/, to: '/complex.html' },
      ]
    }
  }
};