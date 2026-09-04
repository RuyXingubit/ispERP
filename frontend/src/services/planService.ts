import { getPlans } from '../api/generated/endpoints/plans/plans';
import type {
  PlanResponse,
  PlanCreateRequest,
  PlanUpdateRequest,
} from '../api/generated/models';

export type Plan = PlanResponse;
export type { PlanCreateRequest, PlanUpdateRequest };

const plansApi = getPlans();

export const planService = {
  getAll: async (): Promise<PlanResponse[]> => {
    return plansApi.getAllPlans();
  },

  getAllPlans: async (): Promise<PlanResponse[]> => {
    return plansApi.getAllPlans();
  },

  getActivePlans: async (): Promise<PlanResponse[]> => {
    return plansApi.getActivePlans();
  },

  getById: async (id: string): Promise<PlanResponse> => {
    return plansApi.getPlanById(id);
  },

  create: async (planData: PlanCreateRequest): Promise<PlanResponse> => {
    return plansApi.createPlan(planData);
  },

  createPlan: async (planData: PlanCreateRequest): Promise<PlanResponse> => {
    return plansApi.createPlan(planData);
  },

  update: async (id: string, planData: PlanUpdateRequest): Promise<PlanResponse> => {
    return plansApi.updatePlan(id, planData);
  },

  updatePlan: async (id: string, planData: PlanUpdateRequest): Promise<PlanResponse> => {
    return plansApi.updatePlan(id, planData);
  },

  delete: async (id: string): Promise<void> => {
    return plansApi.deletePlan(id);
  },

  deletePlan: async (id: string): Promise<void> => {
    return plansApi.deletePlan(id);
  },
};

export default planService;
