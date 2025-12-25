/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementHashesComponent} from './content-element-hashes.component';
import {TranslatePipe} from "@ngx-translate/core";
import {MockPipe} from "ng-mocks";

describe('ContentElementHashesComponent', () => {
	let component: ContentElementHashesComponent;
	let fixture: ComponentFixture<ContentElementHashesComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				MockPipe(TranslatePipe)
			],
			imports: [
				ContentElementHashesComponent
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementHashesComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});


	it('should accept a non-empty array of HashContentElement as input', () => {
		component.element = [
			{file: 'file-1', hash: 'abc123'},
			{file: 'file-2', hash: 'def456'}
		];
		expect(component.element.length).toBe(2);
		expect(component.element[0].file).toBe('file-1');
	});

	it('should handle an empty array as input', () => {
		component.element = [];
		expect(component.element).toEqual([]);
	});
});