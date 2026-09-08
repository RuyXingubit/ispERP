import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'payment_gateway_model.dart';
import 'payment_gateway_repository.dart';

final paymentGatewaysProvider =
    StateNotifierProvider<PaymentGatewaysNotifier, AsyncValue<List<PaymentGatewayModel>>>((ref) {
  final repo = ref.watch(paymentGatewayRepositoryProvider);
  return PaymentGatewaysNotifier(repo);
});

class PaymentGatewaysNotifier extends StateNotifier<AsyncValue<List<PaymentGatewayModel>>> {
  final PaymentGatewayRepository _repository;

  PaymentGatewaysNotifier(this._repository) : super(const AsyncValue.loading()) {
    loadConfigs();
  }

  Future<void> loadConfigs() async {
    state = const AsyncValue.loading();
    try {
      final configs = await _repository.getConfigs();
      state = AsyncValue.data(configs);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> saveConfig(PaymentGatewayModel config) async {
    try {
      final saved = await _repository.saveConfig(config);
      state.whenData((list) {
        final index = list.indexWhere((item) => item.id == saved.id || item.gatewayType == saved.gatewayType);
        if (index >= 0) {
          final updated = List<PaymentGatewayModel>.from(list);
          updated[index] = saved;
          state = AsyncValue.data(updated);
        } else {
          state = AsyncValue.data([...list, saved]);
        }
      });
    } catch (e) {
      rethrow;
    }
  }
}
