/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {
  NgbModalConfig,
  NgbProgressbarConfig,
} from '@ng-bootstrap/ng-bootstrap';

export const configureNgbComponents =
	(progressbarConfig: NgbProgressbarConfig, modalConfig: NgbModalConfig) =>
		() => {
			progressbarConfig.animated = true;
			progressbarConfig.striped = true;
			progressbarConfig.showValue = false;
			progressbarConfig.type = 'dark';
			progressbarConfig.height = '1rem';

			modalConfig.keyboard = false;
			modalConfig.backdrop = 'static';
			modalConfig.size = 'lg';
			modalConfig.centered = true;
		};
