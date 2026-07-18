'use strict';

const {
  invokeOsbClient,
  listTemplates,
  listClientInstances,
  templateOptions,
  clientOptions,
  pickOsbPayload,
  firstId,
} = require('../shared/osbClient');

/**
 * Execute an HTTP_REQUEST template via a configured OSB HTTP client.
 * Method/path/body come from the template JSON after placeholder rendering.
 */
class OsbHttp {
  constructor() {
    this.description = {
      displayName: 'OSB HTTP',
      name: 'osbHttp',
      icon: 'fa:globe',
      group: ['transform'],
      version: 1,
      description: 'Render an HTTP template and call it via an OSB HTTP client',
      defaults: { name: 'OSB HTTP' },
      inputs: ['main'],
      outputs: ['main'],
      properties: [
        {
          displayName: 'Template',
          name: 'templateId',
          type: 'options',
          typeOptions: { loadOptionsMethod: 'getHttpTemplates' },
          required: true,
          default: '',
          description:
            'HTTP request template (Admin → Templates), e.g. {"method":"GET","path":"/get",...}',
        },
        {
          displayName: 'HTTP Client',
          name: 'clientId',
          type: 'options',
          typeOptions: { loadOptionsMethod: 'getHttpClients' },
          default: '',
          description:
            'Target HTTP client. Empty = first client from the workflow definition.',
        },
      ],
    };

    this.methods = {
      loadOptions: {
        async getHttpClients() {
          try {
            return clientOptions(await listClientInstances(this, 'HTTP'));
          } catch (err) {
            return [{ name: `Error: ${err.message}`, value: '' }];
          }
        },
        async getHttpTemplates() {
          try {
            const http = await listTemplates(this, 'HTTP_REQUEST');
            const text = await listTemplates(this, 'TEXT');
            return templateOptions([...http, ...text], '— select template —');
          } catch (err) {
            return [{ name: `Error: ${err.message}`, value: '' }];
          }
        },
      },
    };
  }

  async execute() {
    const items = this.getInputData();
    const out = [];
    for (let i = 0; i < items.length; i++) {
      const input = items[i]?.json || {};
      const osb = pickOsbPayload(input);

      let templateId = this.getNodeParameter('templateId', i, '');
      if (!templateId) {
        const templates = osb.templates || input.templates || [];
        const match = templates.find(
          (t) => t && (t.kind === 'HTTP_REQUEST' || t.kind === 'TEXT'),
        );
        templateId = (match && match.id) || '';
      }
      if (!templateId) {
        throw new Error('OSB HTTP: select a Template');
      }

      let clientId = this.getNodeParameter('clientId', i, '');
      if (!clientId) {
        clientId = firstId(osb.httpClientIds || input.httpClientIds);
      }

      out.push(
        await invokeOsbClient(this, 'HTTP', i, {
          action: 'request',
          clientId,
          templateId,
        }),
      );
    }
    return [out];
  }
}

module.exports = { OsbHttp };
