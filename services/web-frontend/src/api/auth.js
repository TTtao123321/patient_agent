import request from '../utils/request';

export function login(username, password) {
  return request({
    url: '/v1/users/login',
    method: 'post',
    data: {
      username,
      password,
    },
  });
}
