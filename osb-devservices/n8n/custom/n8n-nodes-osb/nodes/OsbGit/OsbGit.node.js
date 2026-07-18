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
 * Run a Git file template via Gitea HTTP API.
 * Action from webhook kind: PROVISION/UPDATE→commit, DEPROVISION→delete, else status.
 */
class OsbGit {
  constructor() {
    this.description = {
      displayName: 'OSB Git',
      name: 'osbGit',
      icon: 'fa:code-branch',
      group: ['transform'],
      version: 1,
      description: 'Commit/status/delete a file template in a Gitea repository',
      defaults: { name: 'OSB Git' },
      inputs: ['main'],
      outputs: ['main'],
      properties: [
        {
          displayName: 'Template',
          name: 'templateId',
          type: 'options',
          typeOptions: { loadOptionsMethod: 'getGitTemplates' },
          required: true,
          default: '',
          description: 'Git file template JSON: path, content, message',
        },
        {
          displayName: 'Git Client',
          name: 'clientId',
          type: 'options',
          typeOptions: { loadOptionsMethod: 'getGitClients' },
          default: '',
          description: 'Gitea HTTPS client. Empty = first from workflow.',
        },
      ],
    };

    this.methods = {
      loadOptions: {
        async getGitClients() {
          try {
            return clientOptions(await listClientInstances(this, 'GIT'));
          } catch (err) {
            return [{ name: `Error: ${err.message}`, value: '' }];
          }
        },
        async getGitTemplates() {
          try {
            const git = await listTemplates(this, 'GIT_COMMAND');
            const text = await listTemplates(this, 'TEXT');
            return templateOptions([...git, ...text], '— select template —');
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
      const kind = String(osb.kind || input.kind || '').toUpperCase();
      let action = 'status';
      if (kind === 'DEPROVISION' || kind === 'UNBIND') action = 'delete';
      else if (kind === 'PROVISION' || kind === 'UPDATE' || kind === 'BIND') action = 'commit';

      let templateId = this.getNodeParameter('templateId', i, '');
      if (!templateId) {
        const templates = osb.templates || input.templates || [];
        const match = templates.find(
          (t) => t && (t.kind === 'GIT_COMMAND' || t.kind === 'TEXT'),
        );
        templateId = (match && match.id) || '';
      }
      if (!templateId) {
        throw new Error('OSB Git: select a Template');
      }

      let clientId = this.getNodeParameter('clientId', i, '');
      if (!clientId) {
        clientId = firstId(osb.gitClientIds || input.gitClientIds);
      }

      out.push(
        await invokeOsbClient(this, 'GIT', i, {
          action,
          clientId,
          templateId,
        }),
      );
    }
    return [out];
  }
}

module.exports = { OsbGit };
