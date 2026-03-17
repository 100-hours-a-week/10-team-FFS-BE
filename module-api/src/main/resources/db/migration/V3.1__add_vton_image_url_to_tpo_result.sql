ALTER TABLE `tpo_result`
    ADD COLUMN `vton_image_url` VARCHAR(512) NULL COMMENT '가상 착용 이미지 URL' AFTER `outfit_id`;