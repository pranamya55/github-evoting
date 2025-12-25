# Voter Portal

The voter-portal provides a front-end for the voter and covers the entire voting process:

- confirming the legal conditions,
- entering the start voting key,
- selecting voting options,
- confirming the selection of voting options,
- displaying the Choice Return Codes for every selected voting option,
- entering the Ballot Casting Key,
- displaying the Vote Cast Return Code,
- providing a help menu to the voter.

Moreover, the voter-portal supports four languages (German, French, Italian, Rumantsch) and
authentication factors in addition to the Start Voting Key (date of birth, year of birth).

The voter-portal supports multiple electoral models:

- Referendums / Initiatives (allowing the voter to answer yes, no, or empty for specific questions)
- Elections with lists and candidates, with the possibility of accumulating candidates, list combinations,
  and candidates appearing on multiple lists
- Elections without lists

The voter-portal builds upon the voting-client, which implements the voting client's
cryptographic algorithms.

## Usage

The voter portal is packaged as an Angular application.

In our productive infrastructure, we deploy the voter portal behind a reverse proxy, which makes sure that the
following [HTTP Response Headers](https://owasp.org/www-project-secure-headers) are set:

| HTTP Response Header         | Value                                                                                                                                                                                                                                                   |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Cache-Control                | no-cache; no-store; must-revalidate;max-age=0                                                                                                                                                                                                           |
| Content-Disposition          | Inline                                                                                                                                                                                                                                                  |
| Content-Security-Policy      | default-src 'none'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; worker-src 'self'; frame-src 'none'; font-src 'self'; base-uri 'self'; frame-ancestors 'none'; form-action 'none' |
| Content-Type                 | Always add charset=utf-8 for HTML, JSON, Javascript and CSS resources                                                                                                                                                                                   |
| Cross-Origin-Embedder-Policy | require-corp                                                                                                                                                                                                                                            |
| Cross-Origin-Opener-Policy   | same-origin                                                                                                                                                                                                                                             |
| Cross-Origin-Resource-Policy | same-origin                                                                                                                                                                                                                                             |
| Permission-policy            | vibrate=(), microphone=(), geolocation=(), camera=(), display-capture=()                                                                                                                                                                                |
| Pragma                       | no-cache                                                                                                                                                                                                                                                |
| Referrer-Policy              | no-referrer                                                                                                                                                                                                                                             |
| Strict-Transport-Security    | max-age=63072000; includeSubDomains; preload                                                                                                                                                                                                            |
| X-Content-Type-Options       | nosniff                                                                                                                                                                                                                                                 |
| X-Frame-Options              | DENY                                                                                                                                                                                                                                                    |

Our content security policy (CSP) differs from a more strict policy in the following parameters: we allow wasm-unsafe-eval and unsafe-inline for
style-src.
While CSP is designed to disable certain features, we need to integrate a customizable voter portal and load WebAssembly
code, which depend on these features.
It's important to note that for the Swiss Post Voting System, the CSP headers serve as a defense-in-depth mechanism.
However, verifiability and privacy are not reliant on them.

Moreover, our productive infrastructure enforces best practices in server configuration such
as [DNSSEC](https://www.nic.ch/security/dnssec/)
, [OCSP](https://www.ietf.org/rfc/rfc2560.txt), and [CAA records](https://support.dnsimple.com/articles/caa-record/).

## Configuration

The Voter Portal requires specific configuration keys for proper initialization.

These configuration values are stored in a JSON file, which is uploaded to the Voting Server through the Secure Data Manager during the setup phase.

Along with this file, the canton logo and favicon are also uploaded. The Voter Portal retrieves this configuration using the election event ID during
the voting phase.

This configuration file should contain various parameters described below.

### Extended authentication factor (mandatory)

To render the corresponding extended authentication factor UI component, the `identification` key must be configured
with `dob` or `yob`.

| Value | Description                                                      |
|-------|------------------------------------------------------------------|
| `dob` | "date of birth" rendered as a full date with `DD.MM.YYYY` format |
| `yob` | "year of birth" rendered as a date with `YYYY` format            |

```json5
{
	"identification": "dob"
}
```

### Contests capabilities (mandatory)

The contests capabilities are configured with the `contestsCapabilities` key.

| Contests capabilities node key | Data type | Description                                                             |
|--------------------------------|-----------|-------------------------------------------------------------------------|
| writeIns                       | `boolean` | `true` if the voter portal supports elections with write-in candidates. |

```json5
{
	"contestsCapabilities": {
		"writeIns": true
	}
}
```

### Request timeout (mandatory)

The Voter Portal displays a timeout page if a request exceeds the configured timeout duration. These timeouts are configured with
the `requestTimeout` key. All durations are in milliseconds, e.g. 30000 for 30 seconds.

Note: The duration should be equal to or smaller than the `responseCompletion.defaultTimeout` value defined in the Voting Server.

| Request timeout node key | Data type | Description                                     |
|--------------------------|-----------|-------------------------------------------------|
| authenticateVoter        | `number`  | Timeout duration of the authentication request. |
| sendVote                 | `number`  | Timeout duration of the send vote request.      |
| confirmVote              | `number`  | Timeout duration of the confirm vote request.   |

```json5
{
	"requestTimeout": {
		"authenticateVoter": 30000,
		"sendVote": 120000,
		"confirmVote": 120000
	}
}
```

### Translation Placeholders (mandatory)

The translations are customizable with the `translatePlaceholders` key.
All the values are mandatory, if a value is not defined for an election event please fill it an empty string `""`.

| Legal terms node key   | Data type | Description                                                                                                                                     |
|------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| logo_title             | `Text`    | Title of the logo.                                                                                                                              |
| voting_card_{n}        | `Text`    | These placeholders define canton-specific variations of the term 'Voting Card'. <br/>{n} must be between 1 and 6.                               |
| support                | `Text`    | The contact address for voter support.                                                                                                          |
| voting_card_remark_{n} | `Text`    | Placeholders designated for clarifications specific to each canton about the availability of voting material. <br/>{n} must be between 1 and 3. |
| `Text`.{lang}          | `string`  | The language must be any of [`DE`, `FR`, `IT`, `RM`].                                                                                           |

```json5
{
	"translatePlaceholders": {
		"logo_title": {
			"DE": "Die Post",
			"FR": "La Poste",
			"IT": "La Posta",
			"RM": "La Posta"
		},
		"voting_card_1": {
			...
		},
		"voting_card_2": {
			...
		},
		"voting_card_3": {
			...
		},
		"voting_card_4": {
			...
		},
		"voting_card_5": {
			...
		},
		"voting_card_6": {
			...
		},
		"support": {
			...
		},
		"voting_card_remark_1": {
			...
		},
		"voting_card_remark_2": {
			...
		},
		"voting_card_remark_3": {
			...
		},
	}
}
```

To see where these keys are used you can have a look at the translation files `[LOCALE].json` located
in `webapps/ROOT/assets/i18n/`.

### Header Customization (mandatory)

The Voter Portal header can be customized to display for example a specific logo. Here are the customizable keys in
the `header` node.

The logo and the favicon are uploaded to the Voting Server via the Secure Data Manager at the setup phase.

| Header node key       | Data type                          | Description                                                                                                                 |
|-----------------------|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| logoHeight            | `{desktop:number, mobile: number}` | Height of the logo in pixels.                                                                                               |
| reverse (optional)    | `boolean`                          | If `true` the header is reversed, the logo appears on the right. Default is `false`.                                        |
| background (optional) | `string`                           | CSS background definition (i.e. `linear-gradient(180deg,#f7f7f7 0,#ebebeb)`). Default is `none`.                            |
| bars (optional)       | `Bar[]`                            | A `Bar` represents a line displayed below the navigation bar. It has a `height` and a `color` properties. Default is empty. |
| `Bar`.height          | `{desktop:number, mobile: number}` | Height of the bar in pixels.                                                                                                |
| `Bar`.color           | `string`                           | Hexadecimal color value (i.e. `#000000`)                                                                                    |

```json5
{
	"header": {
		"logoHeight": {
			"desktop": 72,
			"mobile": 56
		},
		"reverse": false,
		"background": "#fff",
		"bars": [
			{
				"height": {
					"desktop": 2,
					"mobile": 2
				},
				"color": "#000000"
			}
		]
	}
}
```

### Additional Legal Terms (optional)

By configuring an `additionalLegalTerms` node, you add additional legal terms to the Legal Terms page. These additional
legal terms are added before the default legal terms.

Here are the customizable keys in the `additionalLegalTerms` node.

| Legal terms node key            | Data type            | Description                                                                                                                                                                |
|---------------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| mainTitle                       | `Text`               | Title of the legal terms.                                                                                                                                                  |
| sections                        | `LegalTermSection[]` | An array of LegalTermSection.                                                                                                                                              |
| `LegalTermSection`.sectionTitle | `Text`               | Title of the legal terms section.                                                                                                                                          |
| `LegalTermSection`.terms        | `Text[]`             | Array of legal terms for the section. Each term is displayed on a new line and can optionally be supplemented with the help of [Markdown](https://www.markdownguide.org/). |
| `LegalTermSection`.confirm      | `Text`               | Label of the legal terms confirmation checkbox for the section.                                                                                                            |
| `Text`.{lang}                   | `string`             | The language must be any of [`DE`, `FR`, `IT`, `RM`]. Contains a `LegalTerm`.                                                                                              |

```json5
{
	"additionalLegalTerms": {
		"mainTitle": {
			"DE": "Title of the legal terms",
			"FR": "Title of the legal terms",
			"IT": "Title of the legal terms",
			"RM": "Title of the legal terms",
		},
		"sections": [
			{
				"sectionTitle": {
					"DE": "Title of the additional legal terms",
					...
				},
				"terms": [
					{
						"DE": "First legal terms paragraph, you can write **bold** or *italic*.",
						...
					},
					{
						"DE": "Second paragraph with a [link](https://www.your-website.com).",
						...
					},
					{
						"DE": "Third paragraph with bullets \n - Bullet 1 \n - Bullet 2}",
						...
					}
				],
				"confirm": {
					"DE": "Legal terms agreement confirmation checkbox label",
					...
				}
			},
			{
				...
			}
		]
	}
}
```

### Additional FAQs (optional)

By configuring an `additionalFAQs` node, you add additional FAQ sections to the Help page. These additional FAQ sections are added at the top of the FAQ
sections list in the order defined in this configuration.

Here are the customizable keys in the `additionalFAQs` node.

| Legal terms node key | Data type | Description                                                                                                                                                                      |
|----------------------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| faqTitle             | `Text`    | Title of the additional FAQ section.                                                                                                                                             |
| content              | `Text[]`  | Array of additional FAQ section content. Each content is displayed on a new line and can optionally be supplemented with the help of [Markdown](https://www.markdownguide.org/). |
| `Text`.{lang}        | `string`  | The language must be any of [`DE`, `FR`, `IT`, `RM`]. Contains an array of `FAQSectionContent`.                                                                                  |

```json5
{
	"additionalFAQs": [
		{
			"faqTitle": {
				"DE": "FAQ Title 1",
				"FR": "FAQ Title 1",
				"IT": "FAQ Title 1",
				"RM": "FAQ Title 1"
			},
			"content": [
				{
					"DE": "For example, you can write **bold** or *italic*.",
					...
				},
				{
					"DE": "And add a second paragraph with a [link](https://www.your-website.com)",
					...
				},
				{
					"DE": " - Also use bullet point. \n - With a second bullet point.",
					...
				}
			]
		},
		{
			"faqTitle": {
				"DE": "FAQ Title 2",
				...
			},
			"content": [
				...
			]
		},
		{
			...
		}
	]
}
```

## Development

Check the build instructions in the readme of the repository 'e-voting' for compiling the voter-portal.

The file `package.json` contains the script section for building and test the Voter Portal.
