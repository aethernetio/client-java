const path = require('path');

module.exports = {
  mode: 'development',
  entry: './src/index.ts',
  module: {
    rules: [
      {
        test: /\.tsx?$/, // This test will match both .ts and .tsx files
        use: 'ts-loader',
        exclude: /node_modules/,
      },
    ],
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.js'],

    // --- ADDED SECTION ---
    // This is required for Webpack 5+
    fallback: {
      // We are not using bcryptjs, so we don't need a polyfill for 'crypto'.
      // Setting this to 'false' tells Webpack to ignore it,
      // resolving the build error from aether-client's dependencies.
      "crypto": false
    }
  },
  output: {
    filename: 'bundle.js',
    path: path.resolve(__dirname, 'dist'),
  },

  // --- ADDED SECTION ---
  // Configuration for the 'webpack serve' (npm start) command
  devServer: {
    // This tells the dev server to serve files from the project root directory.
    // This is crucial so it can find and serve 'index.html'.
    static: {
      directory: path.join(__dirname, './dist'),
    },
    // Automatically open the browser when the server starts
    open: true,
  }
};