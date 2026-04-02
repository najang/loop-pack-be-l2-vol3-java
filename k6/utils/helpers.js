export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function authHeaders(loginId, password) {
    return {
        'Content-Type': 'application/json',
        'X-Loopers-LoginId': loginId,
        'X-Loopers-LoginPw': password,
    };
}

export function authHeadersWithToken(loginId, password, entryToken) {
    return {
        ...authHeaders(loginId, password),
        'X-Entry-Token': entryToken,
    };
}

export function check2xx(res, tag) {
    if (res.status < 200 || res.status >= 300) {
        console.error(`[${tag}] unexpected status ${res.status}: ${res.body}`);
        return false;
    }
    return true;
}