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
 * Apply or delete a Kubernetes template against a configured OSB client.
 * Operation is derived from the webhook kind (DEPROVISION → delete, else apply).
 * Placeholders in the template are filled from the webhook command.
 */
class OsbKubernetes {
  constructor() {
    this.description = {
      displayName: 'OSB Kubernetes',
      name: 'osbKubernetes',
      icon: 'fa:dharmachakra',
      group: ['transform'],
      version: 1,
      description:
        'Render a Kubernetes template and apply/delete it via an OSB Kubernetes client',
      defaults: { name: 'OSB Kubernetes' },
      inputs: ['main'],
      outputs: ['main'],
      properties: [
        {
          displayName: 'Template',
          name: 'templateId',
          type: 'options',
          typeOptions: { loadOptionsMethod: 'getKubernetesTemplates' },
          required: true,
          default: '',
          description:
            'Kubernetes YAML template (Admin → Templates). ${instanceId} etc. come from the webhook.',
        },
        {
          displayName: 'Kubernetes Client',
          name: 'clientId',
          type: 'options',
          typeOptions: { loadOptionsMethod: 'getKubernetesClients' },
          default: '',
          description:
            'Target cluster. Empty = first client from the workflow definition.',
        },
      ],
    };

    this.methods = {
      loadOptions: {
        async getKubernetesClients() {
          try {
            return clientOptions(await listClientInstances(this, 'KUBERNETES'));
          } catch (err) {
            return [{ name: `Error: ${err.message}`, value: '' }];
          }
        },
        async getKubernetesTemplates() {
          try {
            const k8s = await listTemplates(this, 'KUBERNETES_RESOURCE');
            const text = await listTemplates(this, 'TEXT');
            return templateOptions([...k8s, ...text], '— select template —');
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
      let action = 'apply';
      if (kind === 'DEPROVISION') action = 'delete';
      else if (
        kind === 'GET_INSTANCE' ||
        kind === 'INSTANCE_LAST_OPERATION' ||
        kind === 'GET_BINDING' ||
        kind === 'BINDING_LAST_OPERATION'
      ) {
        action = 'get';
      }

      let templateId = this.getNodeParameter('templateId', i, '');
      if (!templateId) {
        const templates = osb.templates || input.templates || [];
        const match = templates.find(
          (t) => t && (t.kind === 'KUBERNETES_RESOURCE' || t.kind === 'TEXT'),
        );
        templateId = (match && match.id) || firstId(osb.templateIds || input.templateIds);
      }
      if (!templateId) {
        throw new Error('OSB Kubernetes: select a Template');
      }

      let clientId = this.getNodeParameter('clientId', i, '');
      if (!clientId) {
        clientId = firstId(osb.kubernetesClientIds || input.kubernetesClientIds);
      }

      const command = osb.command || input.command || {};
      const namespace = command.instanceId || osb.instanceId || '';

      out.push(
        await invokeOsbClient(this, 'KUBERNETES', i, {
          action,
          clientId,
          templateId,
          namespace,
        }),
      );
    }
    return [out];
  }
}

module.exports = { OsbKubernetes };
