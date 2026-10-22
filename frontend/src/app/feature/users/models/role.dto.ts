import { PageDto } from '@/core/model/page.dto';

/** Subset of RoleResponse the user form needs to pick roles. */
export interface RoleDto {
  id: number;
  name: string;
  description: string | null;
  active: boolean;
}

export type RolePageDto = PageDto<RoleDto>;
