import { defineConfig } from 'orval';

export default defineConfig({
  ispErp: {
    input: '../contracts/openapi/openapi.bundled.json',
    output: {
      mode: 'tags-split',
      target: 'src/api/generated/endpoints',
      schemas: 'src/api/generated/models',
      client: 'axios',
      override: {
        mutator: {
          path: './src/services/api.ts',
          name: 'customInstance',
        },
      },
    },
  },
});
