/**
 * Этот файл выполняется JEST перед запуском тестов.
 * Он добавляет в "глобальную область" (global)
 * API, которые ожидает 'aether-client', но
 * которые отсутствуют в тестовой среде jsdom.
 */
const { TextEncoder, TextDecoder } = require('util');
const WebSocket = require('ws');

// 1. Исправляет "ReferenceError: TextEncoder is not defined"
global.TextEncoder = TextEncoder;
global.TextDecoder = TextDecoder;

// 2. Исправляет "ReferenceError: WebSocket is not defined"
// Мы явно говорим JEST использовать реализацию 'ws'
global.WebSocket = WebSocket;