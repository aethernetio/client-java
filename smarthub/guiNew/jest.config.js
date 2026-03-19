module.exports = {
  extensionsToTreatAsEsm: ['.ts'],
 preset: 'ts-jest',
 /**
 * 'jsdom' нужен для 'aether-client',
 * так как он ожидает 'localStorage'.
 */
  testEnvironment: 'node',

 /**
 * Указывает Jest запустить наш файл-полифилл
 * (test/jest.setup.js) перед выполнением тестов.
 * Это исправит 'TextEncoder is not defined'.
 */
 setupFiles: ['./test/jest.setup.js'],

 transform: {
    '^.+\\.[tj]sx?$': [
      'ts-jest',
      {
        useESM: true,
        tsconfig: 'tsconfig.test.json'
      }
    ]
 },

 /**
 * КЛЮЧЕВОЙ ФИКС для 'ws':
 * Этот параметр заставляет Jest при импорте 'ws'
 * загружать Node.js-версию (index.js),
 * а не браузерную заглушку (browser.js),
 * которая бросает ошибку "ws does not work in the browser".
 */
 testEnvironmentOptions: {
 customExportConditions: ['node', 'node-addons'],
  },
  transformIgnorePatterns: [
    "node_modules/(?!aether-client)"
  ]
}